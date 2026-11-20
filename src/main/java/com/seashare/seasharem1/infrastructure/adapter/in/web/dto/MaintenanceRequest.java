package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ventana del mantenimiento iniciado por el propietario o el administrador. Las fechas son ISO-8601
 * con zona (por ejemplo {@code 2026-11-27T18:00:00-05:00}). El motivo solo lo usa el administrador.
 */
public record MaintenanceRequest(
    @JsonProperty("reason") String reason,
    @JsonProperty("maintenance_start") String maintenanceStart,
    @JsonProperty("maintenance_until") String maintenanceUntil) {}
