package com.delacruz.fragancias.exception;

/** Error de validación relacionado con las reglas del sistema. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String message) {
        super(message);
    }
}
