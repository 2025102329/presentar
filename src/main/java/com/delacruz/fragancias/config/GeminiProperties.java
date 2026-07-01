package com.delacruz.fragancias.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Google Gemini.
 *
 * La API key se recibe desde GEMINI_API_KEY para evitar publicarla
 * dentro del código fuente o enviarla al navegador.
 */
@ConfigurationProperties(prefix = "app.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        boolean enabled
) {
    public boolean isConfigured() {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return false;
        }

        String value = apiKey.trim();
        return value.length() > 20
                && !value.equalsIgnoreCase("PEGA_AQUI_TU_API_KEY")
                && !value.equalsIgnoreCase("COLOCA_AQUI_TU_API_KEY")
                && !value.equalsIgnoreCase("TU_API_KEY_DE_GEMINI");
    }
}
