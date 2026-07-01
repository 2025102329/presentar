package com.delacruz.fragancias.exception;

/** Error claro para recursos que no existen. */
public class NoEncontradoException extends RuntimeException {
    public NoEncontradoException(String message) {
        super(message);
    }
}
