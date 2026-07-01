package com.delacruz.fragancias.dto;

import com.delacruz.fragancias.entity.LadoArbol;
import java.time.LocalDateTime;

/** Vista segura del cliente; evita enviar objetos JPA completos al navegador. */
public record ClienteResponse(
        Long id,
        String codigo,
        String nombreCompleto,
        String abreviatura,
        String dni,
        String telefono,
        String email,
        String direccion,
        String zona,
        Long padreId,
        LadoArbol lado,
        int nivel,
        String explicacionInsercion,
        LocalDateTime fechaRegistro
) {}
