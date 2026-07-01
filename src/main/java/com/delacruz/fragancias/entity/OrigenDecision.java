package com.delacruz.fragancias.entity;

/** Indica si una decisión vino de Gemini, de un registro OpenAI antiguo o del respaldo local. */
public enum OrigenDecision {
    GEMINI,
    OPENAI,
    LOCAL
}
