package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cuerpo de POST /api/v1/fleet/boats/draft (paso 1). Los campos pueden venir vacíos: las reglas de
 * negocio los validan y devuelven los mensajes de la especificación.
 */
public record CreateDraftRequest(
    @JsonProperty("name") String name,
    @JsonProperty("legal_registration") String legalRegistration,
    @JsonProperty("type") String type,
    @JsonProperty("max_capacity") Integer maxCapacity) {}
