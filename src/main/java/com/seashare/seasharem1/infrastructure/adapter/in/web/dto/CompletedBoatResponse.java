package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

/**
 * Embarcación ya registrada (resultado del paso 2). Los decimales viajan como texto.
 * `maintenance_by` (OWNER, ADMIN o nulo) dice quién envió la embarcación a mantenimiento; solo
 * tiene valor en ese estado. `review_requested` es verdadero si el propietario ya pidió la revisión
 * de ese mantenimiento.
 */
public record CompletedBoatResponse(
    @JsonProperty("boat_id") UUID boatId,
    @JsonProperty("name") String name,
    @JsonProperty("legal_registration") String legalRegistration,
    @JsonProperty("type") String type,
    @JsonProperty("max_capacity") int maxCapacity,
    @JsonProperty("status") String status,
    @JsonProperty("maintenance_by") String maintenanceBy,
    @JsonProperty("photo_url") String photoUrl,
    @JsonProperty("base_rate") String baseRate,
    @JsonProperty("port") PortResponse port,
    @JsonProperty("services") ServicesResponse services,
    @JsonProperty("registered_at") String registeredAt,
    @JsonProperty("review_requested") boolean reviewRequested) {

  /** Puerto de atraque. */
  public record PortResponse(
      @JsonProperty("name") String name,
      @JsonProperty("latitude") String latitude,
      @JsonProperty("longitude") String longitude) {}

  /** Servicios adicionales; Capitán y Combustible van siempre incluidos y no se listan. */
  public record ServicesResponse(
      @JsonProperty("life_jackets") boolean lifeJackets,
      @JsonProperty("cooler_with_ice") boolean coolerWithIce,
      @JsonProperty("sound_system") boolean soundSystem,
      @JsonProperty("fishing_gear") boolean fishingGear,
      @JsonProperty("diving_gear") boolean divingGear) {}
}
