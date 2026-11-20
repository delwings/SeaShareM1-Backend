package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cuerpo de POST /api/v1/auth/login. El número de documento viaja como texto y se convierte a
 * número en el controlador.
 */
public record LoginRequest(
    @JsonProperty("document_number")
        @NotBlank(message = "El número de documento es obligatorio")
        @Pattern(regexp = "\\d{1,18}", message = "El número de documento debe tener solo dígitos")
        String documentNumber,
    @JsonProperty("password") @NotBlank(message = "La contraseña es obligatoria")
        String password) {}
