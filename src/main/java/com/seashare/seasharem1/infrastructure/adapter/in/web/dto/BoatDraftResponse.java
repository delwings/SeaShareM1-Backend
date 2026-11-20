package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/** Borrador de embarcación: los datos del paso 1. */
public record BoatDraftResponse(
    @JsonProperty("boat_id") UUID boatId,
    @JsonProperty("name") String name,
    @JsonProperty("legal_registration") String legalRegistration,
    @JsonProperty("type") String type,
    @JsonProperty("max_capacity") int maxCapacity,
    @JsonProperty("status") String status) {}
