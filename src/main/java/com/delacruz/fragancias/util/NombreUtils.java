package com.delacruz.fragancias.util;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/** Funciones pequeñas para mantener consistentes los nombres de clientes. */
public final class NombreUtils {

    private NombreUtils() {
        // Esta clase solo contiene métodos estáticos; no hace falta crear objetos.
    }

    public static String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return Arrays.stream(texto.trim().split("\\s+"))
                .filter(parte -> !parte.isBlank())
                .map(NombreUtils::capitalizar)
                .collect(Collectors.joining(" "));
    }

    public static String nombreCompleto(String nombres, String paterno, String materno) {
        return String.join(" ", limpiar(nombres), limpiar(paterno), limpiar(materno)).trim();
    }

    public static String abreviar(String nombres, String paterno, String materno) {
        String primerNombre = limpiar(nombres).split(" ")[0];
        return primerNombre + " " + inicial(paterno) + ". " + inicial(materno) + ".";
    }

    /**
     * Clave local usada solamente como respaldo cuando no hay API key.
     * Se quitan tildes y se conservan letras en minúscula para comparar de forma estable.
     */
    public static String claveComparacion(String texto) {
        String normalizado = Normalizer.normalize(limpiar(texto), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalizado.toLowerCase(Locale.ROOT);
    }

    private static String inicial(String valor) {
        String limpio = limpiar(valor);
        return limpio.isBlank() ? "?" : limpio.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private static String capitalizar(String palabra) {
        if (palabra.isBlank()) {
            return palabra;
        }
        String minuscula = palabra.toLowerCase(Locale.forLanguageTag("es-PE"));
        return minuscula.substring(0, 1).toUpperCase(Locale.forLanguageTag("es-PE"))
                + minuscula.substring(1);
    }
}
