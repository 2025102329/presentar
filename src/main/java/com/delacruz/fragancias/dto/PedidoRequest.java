package com.delacruz.fragancias.dto;

import com.delacruz.fragancias.entity.PrioridadPedido;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PedidoRequest(
        @NotNull Long clienteId,
        @NotNull Long productoId,
        @Min(1) int cantidad,
        @NotNull PrioridadPedido prioridad
) {}
