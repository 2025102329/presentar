package com.delacruz.fragancias.dto;

/** Nodo recursivo que el frontend utiliza para dibujar el árbol. */
public record ArbolClienteNode(
        Long id,
        String codigo,
        String nombreCompleto,
        String abreviatura,
        String zona,
        int nivel,
        String explicacion,
        ArbolClienteNode izquierda,
        ArbolClienteNode derecha
) {}
