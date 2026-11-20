package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/** Una fila de la lista "Mis Embarcaciones Registradas". */
public record BoatListItemResponse(
    @JsonProperty("boat_id") UUID boatId,
    @JsonProperty("name") String name,
    @JsonProperty("legal_registration") String legalRegistration,
    @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @JsonProperty("is_new") boolean isNew,
    @JsonProperty("registered_at") String registeredAt) {}
