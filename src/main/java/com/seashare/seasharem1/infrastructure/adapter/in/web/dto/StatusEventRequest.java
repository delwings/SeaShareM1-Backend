package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Aviso de estado operativo de M2. Cualquier otro campo (por ejemplo montos) se ignora. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusEventRequest(
    @JsonProperty("event_id") String eventId,
    @JsonProperty("vessel_id") String vesselId,
    @JsonProperty("operational_status") String operationalStatus,
    @JsonProperty("occurred_at") String occurredAt) {}
