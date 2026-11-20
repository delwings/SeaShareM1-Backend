package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Estado operativo con la hora de la consulta y la ventana de mantenimiento. Las dos fechas van
 * siempre en la respuesta y valen {@code null} cuando no hay mantenimiento vigente.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record OperationalStatusResponse(
    @JsonProperty("vessel_id") String vesselId,
    @JsonProperty("operational_status") String operationalStatus,
    @JsonProperty("queried_at") String queriedAt,
    @JsonProperty("maintenance_start") String maintenanceStart,
    @JsonProperty("maintenance_until") String maintenanceUntil) {}
