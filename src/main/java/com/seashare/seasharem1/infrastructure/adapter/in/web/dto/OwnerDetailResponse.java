package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/** Información de un propietario para el Administrador (sin contraseña ni datos internos). */
public record OwnerDetailResponse(
    @JsonProperty("owner_id") UUID ownerId,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("document_number") String documentNumber,
    @JsonProperty("email") String email,
    @JsonProperty("phone") String phone) {}
