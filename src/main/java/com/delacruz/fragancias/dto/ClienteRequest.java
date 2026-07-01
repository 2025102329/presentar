package com.delacruz.fragancias.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Datos que llegan desde el formulario de clientes. */
public record ClienteRequest(
        @NotBlank String nombres,
        @NotBlank String apellidoPaterno,
        @NotBlank String apellidoMaterno,
        String dni,
        String telefono,
        @Email String email,
        @NotBlank String direccion,
        @NotBlank String zona
) {}
