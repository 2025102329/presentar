package com.delacruz.fragancias.dto;

import java.math.BigDecimal;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String marca,
        String familiaOlfativa,
        int presentacionMl,
        BigDecimal precio,
        int stock,
        boolean activo
) {}
