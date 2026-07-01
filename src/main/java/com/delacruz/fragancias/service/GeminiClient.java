package com.delacruz.fragancias.service;

import com.delacruz.fragancias.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

/**
 * Cliente HTTP para Google Gemini Interactions API.
 *
 * El backend conserva la API key. El navegador nunca la recibe.
 * Gemini devuelve JSON estructurado según el esquema solicitado.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public GeminiClient(GeminiProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public boolean estaConfigurado() {
        return properties.isConfigured();
    }

    /**
     * Solicita una respuesta JSON ajustada al esquema recibido.
     * Si Gemini no está configurado o responde con error, se devuelve vacío
     * para que el sistema use su algoritmo local de respaldo.
     */
    public Optional<JsonNode> solicitarJson(
            String instrucciones,
            String entrada,
            String nombreEsquema,
            ObjectNode esquema
    ) {
        if (!estaConfigurado()) {
            return Optional.empty();
        }

        try {
            ObjectNode formato = mapper.createObjectNode();
            formato.put("type", "text");
            formato.put("mime_type", "application/json");
            formato.set("schema", esquema);

            String prompt = instrucciones
                    + "\n\nNombre del resultado: " + nombreEsquema
                    + "\n\nDATOS DE ENTRADA EN JSON:\n" + entrada;

            ObjectNode cuerpo = mapper.createObjectNode();
            cuerpo.put("model", properties.model());
            cuerpo.put("input", prompt);
            cuerpo.put("store", false);
            cuerpo.set("response_format", formato);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.baseUrl()))
                    .timeout(Duration.ofSeconds(90))
                    .header("x-goog-api-key", properties.apiKey().trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(cuerpo)))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Gemini respondió con estado {}. Detalle: {}",
                        response.statusCode(), recortar(response.body()));
                return Optional.empty();
            }

            JsonNode respuestaCompleta = mapper.readTree(response.body());
            String textoSalida = extraerTexto(respuestaCompleta);

            if (textoSalida == null || textoSalida.isBlank()) {
                log.warn("Gemini respondió sin texto JSON utilizable. Respuesta: {}",
                        recortar(response.body()));
                return Optional.empty();
            }

            return Optional.of(mapper.readTree(limpiarBloqueMarkdown(textoSalida)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("La llamada a Gemini fue interrumpida.", ex);
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("No se pudo completar la llamada a Gemini: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Interactions API actual: steps -> model_output -> content -> text.
     * También conserva compatibilidad con respuestas anteriores para facilitar
     * cambios de versión de la API sin romper el sistema.
     */
    private String extraerTexto(JsonNode respuesta) {
        JsonNode steps = respuesta.path("steps");
        if (steps.isArray()) {
            for (int i = steps.size() - 1; i >= 0; i--) {
                JsonNode step = steps.get(i);
                if (!"model_output".equals(step.path("type").asText())) {
                    continue;
                }
                String texto = textoDesdeContenido(step.path("content"));
                if (texto != null) {
                    return texto;
                }
            }
        }

        // Compatibilidad con el esquema anterior de Interactions API.
        JsonNode outputs = respuesta.path("outputs");
        if (outputs.isArray()) {
            for (int i = outputs.size() - 1; i >= 0; i--) {
                JsonNode output = outputs.get(i);
                if ("text".equals(output.path("type").asText())) {
                    String texto = output.path("text").asText(null);
                    if (texto != null && !texto.isBlank()) {
                        return texto;
                    }
                }
            }
        }

        // Compatibilidad con generateContent por si se cambia el endpoint.
        JsonNode candidates = respuesta.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            String texto = textoDesdeContenido(candidates.get(0).path("content").path("parts"));
            if (texto != null) {
                return texto;
            }
        }

        String outputText = respuesta.path("output_text").asText(null);
        if (outputText != null && !outputText.isBlank()) {
            return outputText;
        }

        return null;
    }

    private String textoDesdeContenido(JsonNode contenido) {
        if (!contenido.isArray()) {
            return null;
        }

        StringBuilder resultado = new StringBuilder();
        for (JsonNode parte : contenido) {
            String tipo = parte.path("type").asText();
            if (tipo.isBlank() || "text".equals(tipo)) {
                String texto = parte.path("text").asText(null);
                if (texto != null && !texto.isBlank()) {
                    resultado.append(texto);
                }
            }
        }
        return resultado.isEmpty() ? null : resultado.toString();
    }

    private String limpiarBloqueMarkdown(String texto) {
        String limpio = texto.trim();
        if (limpio.startsWith("```")) {
            limpio = limpio.replaceFirst("^```(?:json)?\\s*", "");
            limpio = limpio.replaceFirst("\\s*```$", "");
        }
        return limpio.trim();
    }

    private String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= 700 ? texto : texto.substring(0, 700) + "...";
    }
}
