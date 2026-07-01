package com.delacruz.fragancias.dto;

import com.delacruz.fragancias.entity.EstadoPedido;
import com.delacruz.fragancias.entity.PrioridadPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoResponse(
        Long id,
        String codigo,
        Long clienteId,
        String clienteCodigo,
        String clienteNombre,
        String clienteAbreviado,
        Long productoId,
        String producto,
        String marca,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal total,
        PrioridadPedido prioridad,
        EstadoPedido estado,
        String direccionEntrega,
        String zonaEntrega,
        LocalDateTime fechaRegistro
) {}
