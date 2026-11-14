package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/** Propietario de una embarcación, tal como lo ve el Administrador. */
public record OwnerResponse(
    @JsonProperty("owner_id") UUID ownerId,
    @JsonProperty("full_name") String fullName,
    @JsonProperty("document_number") String documentNumber) {}
