package com.delacruz.fragancias.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank String nombre,
        @NotBlank String marca,
        @NotBlank String familiaOlfativa,
        @Min(1) int presentacionMl,
        @DecimalMin("0.01") BigDecimal precio,
        @Min(0) int stock
) {}
