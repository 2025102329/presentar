package com.delacruz.fragancias.dto;

import com.delacruz.fragancias.entity.OrigenDecision;

/** Respuesta interna de la IA para una comparación del árbol. */
public record DecisionArbol(
        String decision,
        String explicacion,
        OrigenDecision origen
) {}
