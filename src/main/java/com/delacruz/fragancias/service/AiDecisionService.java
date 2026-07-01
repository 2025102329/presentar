package com.delacruz.fragancias.service;

import com.delacruz.fragancias.dto.DecisionArbol;
import com.delacruz.fragancias.dto.DecisionRuta;
import com.delacruz.fragancias.entity.*;
import com.delacruz.fragancias.repository.DecisionIaRepository;
import com.delacruz.fragancias.util.NombreUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Aquí se concentran las decisiones de inteligencia artificial.
 *
 * El árbol y el grafo llaman a este servicio, pero luego validan la respuesta.
 * De esta forma la IA decide y el sistema evita guardar una estructura rota.
 */
@Service
public class AiDecisionService {

    private final GeminiClient geminiClient;
    private final DecisionIaRepository decisionRepository;
    private final ObjectMapper mapper;

    public AiDecisionService(
            GeminiClient geminiClient,
            DecisionIaRepository decisionRepository,
            ObjectMapper mapper
    ) {
        this.geminiClient = geminiClient;
        this.decisionRepository = decisionRepository;
        this.mapper = mapper;
    }

    public boolean iaConfigurada() {
        return geminiClient.estaConfigurado();
    }

    /**
     * La IA compara dos nombres completos y responde IZQUIERDA, DERECHA o IGUAL.
     * La comparación local solo se usa si la API no está disponible.
     */
    @Transactional
    public DecisionArbol decidirLado(String nuevoNombre, String nombreActual) {
        ObjectNode entrada = mapper.createObjectNode();
        entrada.put("nuevoNombre", nuevoNombre);
        entrada.put("nombreNodoActual", nombreActual);

        ObjectNode esquema = esquemaDecisionArbol();
        String instrucciones = """
                Eres el comparador de un árbol binario de clientes de una perfumería.
                Compara los nombres completos en orden lexicográfico español, de izquierda a derecha.
                Ignora diferencias de mayúsculas, minúsculas, espacios repetidos y tildes.
                Devuelve IZQUIERDA si el nuevo nombre es menor, DERECHA si es mayor e IGUAL si coincide.
                No inventes datos ni cambies los nombres. La explicación debe ser breve y comprensible.
                """;

        Optional<JsonNode> salidaIa = geminiClient.solicitarJson(
                instrucciones,
                entrada.toString(),
                "decision_arbol_cliente",
                esquema
        );

        if (salidaIa.isPresent()) {
            String decision = salidaIa.get().path("decision").asText("").toUpperCase(Locale.ROOT);
            String explicacion = salidaIa.get().path("explicacion").asText("Comparación realizada por Gemini.");

            if (Set.of("IZQUIERDA", "DERECHA", "IGUAL").contains(decision)) {
                guardarAuditoria(TipoDecisionIa.INSERCION_ARBOL, OrigenDecision.GEMINI,
                        entrada, salidaIa.get(), decision, explicacion);
                return new DecisionArbol(decision, explicacion, OrigenDecision.GEMINI);
            }
        }

        // Respaldo transparente: no se presenta como IA si la API no respondió.
        int comparacion = NombreUtils.claveComparacion(nuevoNombre)
                .compareTo(NombreUtils.claveComparacion(nombreActual));
        String decision = comparacion < 0 ? "IZQUIERDA" : comparacion > 0 ? "DERECHA" : "IGUAL";
        String explicacion = "Se aplicó la comparación alfabética local porque la IA no estaba disponible.";

        ObjectNode salidaLocal = mapper.createObjectNode();
        salidaLocal.put("decision", decision);
        salidaLocal.put("explicacion", explicacion);
        guardarAuditoria(TipoDecisionIa.INSERCION_ARBOL, OrigenDecision.LOCAL,
                entrada, salidaLocal, decision, explicacion);

        return new DecisionArbol(decision, explicacion, OrigenDecision.LOCAL);
    }

    /**
     * La IA decide el orden de visita de las zonas. Las distancias recibidas ya
     * fueron calculadas por el grafo, por lo que el modelo trabaja con datos válidos.
     */
    @Transactional
    public DecisionRuta decidirOrdenRuta(
            List<String> zonasObjetivo,
            Map<String, Integer> prioridadPorZona,
            Map<String, Map<String, Double>> distancias
    ) {
        List<String> zonas = zonasObjetivo.stream().distinct().toList();

        ObjectNode entrada = mapper.createObjectNode();
        entrada.set("zonasObjetivo", mapper.valueToTree(zonas));
        entrada.set("prioridadPorZona", mapper.valueToTree(prioridadPorZona));
        entrada.put("escalaPrioridad", "3=URGENTE, 2=ALTA, 1=NORMAL");
        entrada.set("distanciasKm", mapper.valueToTree(distancias));
        entrada.put("inicioYFin", "Tienda");

        String instrucciones = """
                Eres responsable de organizar entregas de perfumes dentro de un grafo.
                Decide el orden de visita más conveniente considerando prioridad y distancia.
                La ruta debe comenzar en Tienda, incluir cada zona objetivo exactamente una vez
                y terminar en Tienda. Solo puedes utilizar nombres presentes en la entrada.
                En prioridadPorZona, 3 significa URGENTE, 2 significa ALTA y 1 significa NORMAL.
                Prioriza 3 sobre 2 y 2 sobre 1, evitando recorridos innecesarios.
                Devuelve una explicación breve y un criterio concreto.
                """;

        Optional<JsonNode> salidaIa = geminiClient.solicitarJson(
                instrucciones,
                entrada.toString(),
                "decision_orden_ruta",
                esquemaDecisionRuta()
        );

        if (salidaIa.isPresent()) {
            List<String> orden = leerListaTexto(salidaIa.get().path("ordenZonas"));
            String criterio = salidaIa.get().path("criterio").asText("Prioridad y distancia");
            String explicacion = salidaIa.get().path("explicacion").asText("Ruta organizada por Gemini.");

            if (ordenValido(orden, zonas)) {
                guardarAuditoria(TipoDecisionIa.ORDEN_RUTA, OrigenDecision.GEMINI,
                        entrada, salidaIa.get(), String.join(" → ", orden), explicacion);
                return new DecisionRuta(orden, criterio, explicacion, OrigenDecision.GEMINI);
            }
        }

        List<String> ordenLocal = construirOrdenLocal(zonas, prioridadPorZona, distancias);
        String explicacion = "Se usó el organizador local porque la IA no estaba disponible o devolvió una ruta inválida.";
        ObjectNode salidaLocal = mapper.createObjectNode();
        salidaLocal.set("ordenZonas", mapper.valueToTree(ordenLocal));
        salidaLocal.put("criterio", "Prioridad y vecino más cercano");
        salidaLocal.put("explicacion", explicacion);

        guardarAuditoria(TipoDecisionIa.ORDEN_RUTA, OrigenDecision.LOCAL,
                entrada, salidaLocal, String.join(" → ", ordenLocal), explicacion);

        return new DecisionRuta(
                ordenLocal,
                "Prioridad y vecino más cercano",
                explicacion,
                OrigenDecision.LOCAL
        );
    }

    private ObjectNode esquemaDecisionArbol() {
        ObjectNode propiedades = mapper.createObjectNode();
        ObjectNode decision = mapper.createObjectNode();
        decision.put("type", "string");
        ArrayNode enumValores = mapper.createArrayNode();
        enumValores.add("IZQUIERDA").add("DERECHA").add("IGUAL");
        decision.set("enum", enumValores);
        propiedades.set("decision", decision);
        propiedades.set("explicacion", mapper.createObjectNode().put("type", "string"));

        ObjectNode esquema = mapper.createObjectNode();
        esquema.put("type", "object");
        esquema.set("properties", propiedades);
        esquema.set("required", mapper.createArrayNode().add("decision").add("explicacion"));
        esquema.put("additionalProperties", false);
        return esquema;
    }

    private ObjectNode esquemaDecisionRuta() {
        ObjectNode propiedades = mapper.createObjectNode();
        ObjectNode orden = mapper.createObjectNode();
        orden.put("type", "array");
        orden.set("items", mapper.createObjectNode().put("type", "string"));
        propiedades.set("ordenZonas", orden);
        propiedades.set("criterio", mapper.createObjectNode().put("type", "string"));
        propiedades.set("explicacion", mapper.createObjectNode().put("type", "string"));

        ObjectNode esquema = mapper.createObjectNode();
        esquema.put("type", "object");
        esquema.set("properties", propiedades);
        esquema.set("required", mapper.createArrayNode()
                .add("ordenZonas").add("criterio").add("explicacion"));
        esquema.put("additionalProperties", false);
        return esquema;
    }

    private List<String> leerListaTexto(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> valores = new ArrayList<>();
        node.forEach(item -> valores.add(item.asText()));
        return valores;
    }

    private boolean ordenValido(List<String> orden, List<String> zonasObjetivo) {
        if (orden.size() != zonasObjetivo.size() + 2) {
            return false;
        }
        if (!"Tienda".equalsIgnoreCase(orden.get(0))
                || !"Tienda".equalsIgnoreCase(orden.get(orden.size() - 1))) {
            return false;
        }

        Set<String> esperadas = new HashSet<>(zonasObjetivo);
        Set<String> intermedias = new HashSet<>(orden.subList(1, orden.size() - 1));
        return esperadas.equals(intermedias)
                && orden.subList(1, orden.size() - 1).size() == intermedias.size();
    }

    private List<String> construirOrdenLocal(
            List<String> zonas,
            Map<String, Integer> prioridades,
            Map<String, Map<String, Double>> distancias
    ) {
        List<String> pendientes = new ArrayList<>(zonas);
        List<String> orden = new ArrayList<>();
        orden.add("Tienda");
        String actual = "Tienda";

        while (!pendientes.isEmpty()) {
            final String origen = actual;
            String siguiente = pendientes.stream()
                    .min(Comparator
                            .comparingInt((String zona) -> -prioridades.getOrDefault(zona, 1))
                            .thenComparingDouble(zona -> distancias
                                    .getOrDefault(origen, Map.of())
                                    .getOrDefault(zona, Double.MAX_VALUE)))
                    .orElseThrow();
            orden.add(siguiente);
            pendientes.remove(siguiente);
            actual = siguiente;
        }
        orden.add("Tienda");
        return orden;
    }

    private void guardarAuditoria(
            TipoDecisionIa tipo,
            OrigenDecision origen,
            JsonNode entrada,
            JsonNode salida,
            String decision,
            String explicacion
    ) {
        DecisionIa registro = new DecisionIa();
        registro.setTipo(tipo);
        registro.setOrigen(origen);
        registro.setEntradaJson(entrada.toString());
        registro.setSalidaJson(salida.toString());
        registro.setDecisionFinal(decision);
        registro.setExplicacion(explicacion);
        decisionRepository.save(registro);
    }
}
