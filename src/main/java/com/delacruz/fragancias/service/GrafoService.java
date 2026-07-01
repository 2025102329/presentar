package com.delacruz.fragancias.service;

import com.delacruz.fragancias.dto.*;
import com.delacruz.fragancias.entity.*;
import com.delacruz.fragancias.exception.ReglaNegocioException;
import com.delacruz.fragancias.repository.ConexionRutaRepository;
import com.delacruz.fragancias.repository.RutaPlanificadaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Representa las zonas como un grafo ponderado.
 *
 * La IA decide el orden de visita; Dijkstra valida y completa cada tramo para
 * que la ruta final siempre utilice conexiones que realmente existen.
 */
@Service
public class GrafoService {

    private final ConexionRutaRepository conexionRepository;
    private final RutaPlanificadaRepository rutaRepository;
    private final PedidoService pedidoService;
    private final AiDecisionService aiDecisionService;
    private final HistorialService historialService;
    private final ObjectMapper mapper;

    public GrafoService(
            ConexionRutaRepository conexionRepository,
            RutaPlanificadaRepository rutaRepository,
            PedidoService pedidoService,
            AiDecisionService aiDecisionService,
            HistorialService historialService,
            ObjectMapper mapper
    ) {
        this.conexionRepository = conexionRepository;
        this.rutaRepository = rutaRepository;
        this.pedidoService = pedidoService;
        this.aiDecisionService = aiDecisionService;
        this.historialService = historialService;
        this.mapper = mapper;
    }

    public GrafoResponse obtenerGrafo() {
        List<ConexionRuta> conexiones = conexionRepository.findAllByOrderByOrigenAscDestinoAsc();
        Set<String> nodos = new TreeSet<>();
        conexiones.forEach(c -> {
            nodos.add(c.getOrigen());
            nodos.add(c.getDestino());
        });

        List<GrafoResponse.Arista> aristas = conexiones.stream()
                .map(c -> new GrafoResponse.Arista(
                        c.getId(), c.getOrigen(), c.getDestino(), c.getDistanciaKm(),
                        c.getTiempoMinutos(), c.getTrafico()))
                .toList();
        return new GrafoResponse(new ArrayList<>(nodos), aristas);
    }

    @Transactional
    public RutaResponse generarRuta(RutaRequest request) {
        List<Pedido> pedidos = pedidoService.pedidosParaRuta(
                request == null ? List.of() : request.pedidoIds());

        List<String> zonas = pedidos.stream()
                .map(Pedido::getZonaEntrega)
                .distinct()
                .toList();

        Map<String, Integer> prioridadPorZona = construirPrioridades(pedidos);
        Map<String, Map<String, Double>> matrizDistancias = construirMatriz(zonas);

        DecisionRuta decision = aiDecisionService.decidirOrdenRuta(
                zonas, prioridadPorZona, matrizDistancias);

        List<String> recorridoCompleto = new ArrayList<>();
        double distanciaTotal = 0;
        int tiempoTotal = 0;

        for (int i = 0; i < decision.ordenZonas().size() - 1; i++) {
            ResultadoCamino tramo = dijkstra(
                    decision.ordenZonas().get(i),
                    decision.ordenZonas().get(i + 1));

            if (recorridoCompleto.isEmpty()) {
                recorridoCompleto.addAll(tramo.camino());
            } else {
                // El primer nodo del tramo ya es el último del tramo anterior.
                recorridoCompleto.addAll(tramo.camino().subList(1, tramo.camino().size()));
            }
            distanciaTotal += tramo.distancia();
            tiempoTotal += tramo.tiempoMinutos();
        }

        Map<String, List<String>> entregas = pedidos.stream()
                .collect(Collectors.groupingBy(
                        Pedido::getZonaEntrega,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                p -> p.getCliente().getAbreviatura() + " · " + p.getCodigo(),
                                Collectors.toList())
                ));

        RutaPlanificada ruta = new RutaPlanificada();
        ruta.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 8));
        ruta.setOrdenZonasJson(escribirJson(decision.ordenZonas()));
        ruta.setRecorridoCompletoJson(escribirJson(recorridoCompleto));
        ruta.setEntregasJson(escribirJson(entregas));
        ruta.setDistanciaTotalKm(redondear(distanciaTotal));
        ruta.setTiempoTotalMinutos(tiempoTotal);
        ruta.setCriterio(decision.criterio());
        ruta.setExplicacion(decision.explicacion());
        ruta.setOrigenDecision(decision.origen());
        rutaRepository.save(ruta);
        ruta.setCodigo("RUT-%04d".formatted(ruta.getId()));
        rutaRepository.save(ruta);

        pedidoService.marcarEnRuta(pedidos);
        historialService.apilar("GRAFO",
                "Se generó " + ruta.getCodigo() + " con " + pedidos.size() + " entrega(s). ");

        return mapearRuta(ruta);
    }

    public RutaResponse ultimaRuta() {
        return rutaRepository.findFirstByOrderByFechaDesc()
                .map(this::mapearRuta)
                .orElse(null);
    }

    private Map<String, Integer> construirPrioridades(List<Pedido> pedidos) {
        Map<String, Integer> prioridades = new LinkedHashMap<>();
        for (Pedido pedido : pedidos) {
            int valor = switch (pedido.getPrioridad()) {
                case URGENTE -> 3;
                case ALTA -> 2;
                case NORMAL -> 1;
            };
            prioridades.merge(pedido.getZonaEntrega(), valor, Math::max);
        }
        return prioridades;
    }

    private Map<String, Map<String, Double>> construirMatriz(List<String> zonas) {
        List<String> puntos = new ArrayList<>();
        puntos.add("Tienda");
        puntos.addAll(zonas);

        Map<String, Map<String, Double>> matriz = new LinkedHashMap<>();
        for (String origen : puntos) {
            Map<String, Double> fila = new LinkedHashMap<>();
            for (String destino : puntos) {
                fila.put(destino, origen.equals(destino) ? 0 : dijkstra(origen, destino).distancia());
            }
            matriz.put(origen, fila);
        }
        return matriz;
    }

    /** Dijkstra clásico sobre conexiones bidireccionales. */
    private ResultadoCamino dijkstra(String origen, String destino) {
        Map<String, List<AristaInterna>> grafo = construirAdyacencia();
        if (!grafo.containsKey(origen) || !grafo.containsKey(destino)) {
            throw new ReglaNegocioException("La zona '" + origen + "' o '" + destino + "' no existe en el grafo.");
        }

        Map<String, Double> distancia = new HashMap<>();
        Map<String, Integer> tiempo = new HashMap<>();
        Map<String, String> anterior = new HashMap<>();
        grafo.keySet().forEach(nodo -> {
            distancia.put(nodo, Double.POSITIVE_INFINITY);
            tiempo.put(nodo, Integer.MAX_VALUE);
        });
        distancia.put(origen, 0.0);
        tiempo.put(origen, 0);

        PriorityQueue<NodoDistancia> cola = new PriorityQueue<>(Comparator.comparingDouble(NodoDistancia::distancia));
        cola.add(new NodoDistancia(origen, 0));

        while (!cola.isEmpty()) {
            NodoDistancia actual = cola.poll();
            if (actual.distancia() > distancia.get(actual.nombre())) {
                continue;
            }
            if (actual.nombre().equals(destino)) {
                break;
            }

            for (AristaInterna arista : grafo.getOrDefault(actual.nombre(), List.of())) {
                double nuevaDistancia = distancia.get(actual.nombre()) + arista.distancia();
                if (nuevaDistancia < distancia.get(arista.destino())) {
                    distancia.put(arista.destino(), nuevaDistancia);
                    tiempo.put(arista.destino(), tiempo.get(actual.nombre()) + arista.tiempo());
                    anterior.put(arista.destino(), actual.nombre());
                    cola.add(new NodoDistancia(arista.destino(), nuevaDistancia));
                }
            }
        }

        if (Double.isInfinite(distancia.get(destino))) {
            throw new ReglaNegocioException("No existe un camino entre " + origen + " y " + destino + ".");
        }

        LinkedList<String> camino = new LinkedList<>();
        String paso = destino;
        while (paso != null) {
            camino.addFirst(paso);
            paso = anterior.get(paso);
        }
        return new ResultadoCamino(camino, distancia.get(destino), tiempo.get(destino));
    }

    private Map<String, List<AristaInterna>> construirAdyacencia() {
        Map<String, List<AristaInterna>> grafo = new HashMap<>();
        for (ConexionRuta c : conexionRepository.findAll()) {
            grafo.computeIfAbsent(c.getOrigen(), k -> new ArrayList<>())
                    .add(new AristaInterna(c.getDestino(), c.getDistanciaKm(), c.getTiempoMinutos()));
            grafo.computeIfAbsent(c.getDestino(), k -> new ArrayList<>())
                    .add(new AristaInterna(c.getOrigen(), c.getDistanciaKm(), c.getTiempoMinutos()));
        }
        return grafo;
    }

    private RutaResponse mapearRuta(RutaPlanificada ruta) {
        try {
            List<String> orden = mapper.readValue(ruta.getOrdenZonasJson(), new TypeReference<>() {});
            List<String> recorrido = mapper.readValue(ruta.getRecorridoCompletoJson(), new TypeReference<>() {});
            Map<String, List<String>> entregas = mapper.readValue(ruta.getEntregasJson(), new TypeReference<>() {});
            return new RutaResponse(
                    ruta.getId(), ruta.getCodigo(), orden, recorrido,
                    ruta.getDistanciaTotalKm(), ruta.getTiempoTotalMinutos(),
                    ruta.getCriterio(), ruta.getExplicacion(), ruta.getOrigenDecision(),
                    entregas, ruta.getFecha()
            );
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo leer la ruta guardada.", ex);
        }
    }

    private String escribirJson(Object valor) {
        try {
            return mapper.writeValueAsString(valor);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo guardar el detalle de la ruta.", ex);
        }
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private record AristaInterna(String destino, double distancia, int tiempo) {}
    private record NodoDistancia(String nombre, double distancia) {}
    private record ResultadoCamino(List<String> camino, double distancia, int tiempoMinutos) {}
}
