package com.delacruz.fragancias.dto;

import com.delacruz.fragancias.entity.OrigenDecision;
import java.util.List;

/** Orden de zonas elegido por IA antes de expandir los tramos del grafo. */
public record DecisionRuta(
        List<String> ordenZonas,
        String criterio,
        String explicacion,
        OrigenDecision origen
) {}
