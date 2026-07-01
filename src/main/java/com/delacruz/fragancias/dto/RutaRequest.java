package com.delacruz.fragancias.dto;

import java.util.List;

/** Si la lista está vacía, se toman todos los pedidos listos para envío. */
public record RutaRequest(List<Long> pedidoIds) {}
