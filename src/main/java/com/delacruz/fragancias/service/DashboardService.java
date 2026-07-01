package com.delacruz.fragancias.service;

import com.delacruz.fragancias.config.GeminiProperties;
import com.delacruz.fragancias.dto.DashboardResponse;
import com.delacruz.fragancias.entity.EstadoPedido;
import com.delacruz.fragancias.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DashboardService {

    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;
    private final DecisionIaRepository decisionRepository;
    private final GrafoService grafoService;
    private final GeminiProperties geminiProperties;

    public DashboardService(
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            PedidoRepository pedidoRepository,
            DecisionIaRepository decisionRepository,
            GrafoService grafoService,
            GeminiProperties geminiProperties
    ) {
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
        this.decisionRepository = decisionRepository;
        this.grafoService = grafoService;
        this.geminiProperties = geminiProperties;
    }

    public DashboardResponse resumen() {
        BigDecimal ventas = pedidoRepository.findAll().stream()
                .filter(p -> p.getEstado() == EstadoPedido.ENTREGADO)
                .map(p -> p.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String ultimaDecision = decisionRepository.findFirstByOrderByFechaDesc()
                .map(d -> d.getDecisionFinal() + " · " + d.getOrigen())
                .orElse("Aún no hay decisiones registradas");

        return new DashboardResponse(
                clienteRepository.count(),
                productoRepository.findAllByActivoTrueOrderByNombreAsc().size(),
                pedidoRepository.countByEstado(EstadoPedido.PENDIENTE),
                pedidoRepository.countByEstado(EstadoPedido.LISTO_PARA_ENVIO),
                pedidoRepository.countByEstado(EstadoPedido.ENTREGADO),
                ventas,
                geminiProperties.isConfigured(),
                geminiProperties.model(),
                grafoService.ultimaRuta(),
                ultimaDecision
        );
    }
}
