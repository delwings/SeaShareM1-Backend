package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Únicamente el estado operativo, con la hora de la consulta. */
public record OperationalStatusResponse(
    @JsonProperty("vessel_id") String vesselId,
    @JsonProperty("operational_status") String operationalStatus,
    @JsonProperty("queried_at") String queriedAt) {}
