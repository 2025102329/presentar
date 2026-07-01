package com.delacruz.fragancias.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        long clientes,
        long productos,
        long pedidosPendientes,
        long pedidosListos,
        long pedidosEntregados,
        BigDecimal ventasAcumuladas,
        boolean iaConfigurada,
        String modeloIa,
        RutaResponse ultimaRuta,
        String ultimaDecision
) {}
