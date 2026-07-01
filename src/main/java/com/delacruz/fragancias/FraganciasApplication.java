package com.delacruz.fragancias;

import com.delacruz.fragancias.config.GeminiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada de toda la aplicación.
 *
 * Spring Boot levanta el servidor, conecta la base de datos y publica la
 * interfaz web que se encuentra en src/main/resources/static.
 */
@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties.class)
public class FraganciasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraganciasApplication.class, args);
    }
}
