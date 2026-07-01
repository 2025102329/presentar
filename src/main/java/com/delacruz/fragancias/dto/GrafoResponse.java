package com.delacruz.fragancias.dto;

import java.util.List;

/** Datos mínimos para que JavaScript dibuje el grafo. */
public record GrafoResponse(
        List<String> nodos,
        List<Arista> aristas
) {
    public record Arista(
            Long id,
            String origen,
            String destino,
            double distanciaKm,
            int tiempoMinutos,
            String trafico
    ) {}
}
