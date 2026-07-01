package com.delacruz.fragancias.service;

import com.delacruz.fragancias.dto.*;
import com.delacruz.fragancias.entity.Cliente;
import com.delacruz.fragancias.entity.LadoArbol;
import com.delacruz.fragancias.exception.NoEncontradoException;
import com.delacruz.fragancias.exception.ReglaNegocioException;
import com.delacruz.fragancias.repository.ClienteRepository;
import com.delacruz.fragancias.util.NombreUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Gestiona clientes y conserva la forma del árbol decidida por IA. */
@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final AiDecisionService aiDecisionService;
    private final HistorialService historialService;

    public ClienteService(
            ClienteRepository repository,
            AiDecisionService aiDecisionService,
            HistorialService historialService
    ) {
        this.repository = repository;
        this.aiDecisionService = aiDecisionService;
        this.historialService = historialService;
    }

    @Transactional
    public ClienteResponse registrar(ClienteRequest request) {
        String dni = request.dni() == null ? "" : request.dni().trim();
        if (!dni.isBlank() && repository.existsByDniIgnoreCase(dni)) {
            throw new ReglaNegocioException("Ya existe un cliente registrado con ese DNI.");
        }

        String nombres = NombreUtils.limpiar(request.nombres());
        String paterno = NombreUtils.limpiar(request.apellidoPaterno());
        String materno = NombreUtils.limpiar(request.apellidoMaterno());
        String completo = NombreUtils.nombreCompleto(nombres, paterno, materno);

        // Guardamos la raíz anterior antes de crear el nuevo registro.
        Optional<Cliente> raizExistente = repository.findFirstByLado(LadoArbol.RAIZ);

        Cliente cliente = new Cliente();
        cliente.setCodigo("TMP-" + UUID.randomUUID().toString().substring(0, 8));
        cliente.setNombres(nombres);
        cliente.setApellidoPaterno(paterno);
        cliente.setApellidoMaterno(materno);
        cliente.setNombreCompleto(completo);
        cliente.setAbreviatura(NombreUtils.abreviar(nombres, paterno, materno));
        cliente.setDni(dni);
        cliente.setTelefono(valorLimpio(request.telefono()));
        cliente.setEmail(valorLimpio(request.email()));
        cliente.setDireccion(NombreUtils.limpiar(request.direccion()));
        cliente.setZona(NombreUtils.limpiar(request.zona()));
        cliente.setLado(LadoArbol.RAIZ);
        cliente.setNivel(0);
        cliente.setExplicacionInsercion("Primer cliente registrado: ocupa la raíz del árbol.");
        repository.save(cliente);

        cliente.setCodigo("CLI-%04d".formatted(cliente.getId()));

        if (raizExistente.isPresent()) {
            ubicarEnArbol(cliente, raizExistente.get());
        }

        repository.save(cliente);
        historialService.apilar("CLIENTE",
                "Se registró a " + cliente.getNombreCompleto() + " como " + cliente.getCodigo() + ".");
        return mapear(cliente);
    }

    private void ubicarEnArbol(Cliente nuevo, Cliente actual) {
        int pasos = 0;
        StringBuilder recorrido = new StringBuilder();

        while (pasos++ < 200) {
            DecisionArbol decision = aiDecisionService.decidirLado(
                    nuevo.getNombreCompleto(), actual.getNombreCompleto());

            String ladoTexto = decision.decision();
            if ("IGUAL".equals(ladoTexto)) {
                // Si dos personas tienen el mismo nombre, el código automático rompe el empate.
                ladoTexto = "DERECHA";
            }

            LadoArbol lado = "IZQUIERDA".equals(ladoTexto)
                    ? LadoArbol.IZQUIERDA
                    : LadoArbol.DERECHA;

            recorrido.append(nuevo.getAbreviatura())
                    .append(" se comparó con ")
                    .append(actual.getAbreviatura())
                    .append(" y avanzó a la ")
                    .append(lado == LadoArbol.IZQUIERDA ? "izquierda" : "derecha")
                    .append(". ");

            Optional<Cliente> hijo = repository.findByPadreIdAndLado(actual.getId(), lado);
            if (hijo.isEmpty()) {
                nuevo.setPadre(actual);
                nuevo.setLado(lado);
                nuevo.setNivel(actual.getNivel() + 1);
                nuevo.setExplicacionInsercion(recorrido + decision.explicacion());
                return;
            }
            actual = hijo.get();
        }

        throw new ReglaNegocioException("El árbol superó el límite de recorrido permitido.");
    }

    public List<ClienteResponse> listar() {
        return repository.findAllByOrderByFechaRegistroAsc().stream()
                .map(this::mapear)
                .toList();
    }

    public Cliente obtenerEntidad(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No se encontró el cliente solicitado."));
    }

    public ArbolClienteNode obtenerArbol() {
        return repository.findFirstByLado(LadoArbol.RAIZ)
                .map(this::construirNodo)
                .orElse(null);
    }

    private ArbolClienteNode construirNodo(Cliente cliente) {
        ArbolClienteNode izquierda = repository
                .findByPadreIdAndLado(cliente.getId(), LadoArbol.IZQUIERDA)
                .map(this::construirNodo)
                .orElse(null);
        ArbolClienteNode derecha = repository
                .findByPadreIdAndLado(cliente.getId(), LadoArbol.DERECHA)
                .map(this::construirNodo)
                .orElse(null);

        return new ArbolClienteNode(
                cliente.getId(),
                cliente.getCodigo(),
                cliente.getNombreCompleto(),
                cliente.getAbreviatura(),
                cliente.getZona(),
                cliente.getNivel(),
                cliente.getExplicacionInsercion(),
                izquierda,
                derecha
        );
    }

    private ClienteResponse mapear(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getCodigo(),
                cliente.getNombreCompleto(),
                cliente.getAbreviatura(),
                cliente.getDni(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion(),
                cliente.getZona(),
                cliente.getPadre() == null ? null : cliente.getPadre().getId(),
                cliente.getLado(),
                cliente.getNivel(),
                cliente.getExplicacionInsercion(),
                cliente.getFechaRegistro()
        );
    }

    private String valorLimpio(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
