package com.delacruz.fragancias.dto;

import com.delacruz.fragancias.entity.OrigenDecision;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RutaResponse(
        Long id,
        String codigo,
        List<String> ordenZonas,
        List<String> recorridoCompleto,
        double distanciaTotalKm,
        int tiempoTotalMinutos,
        String criterio,
        String explicacion,
        OrigenDecision origenDecision,
        Map<String, List<String>> entregasPorZona,
        LocalDateTime fecha
) {}
