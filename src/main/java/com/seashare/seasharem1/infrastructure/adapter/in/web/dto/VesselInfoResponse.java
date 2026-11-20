package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Contrato acordado con el Módulo 2 para «Proveer información de embarcación». */
public record VesselInfoResponse(
    @JsonProperty("vessel_id") String vesselId,
    @JsonProperty("name") String name,
    @JsonProperty("registration_number") String registrationNumber,
    @JsonProperty("type") String type,
    @JsonProperty("max_capacity") int maxCapacity,
    @JsonProperty("operational_status") String operationalStatus,
    @JsonProperty("photo_url") String photoUrl,
    @JsonProperty("captain_included") boolean captainIncluded,
    @JsonProperty("port") PortInfo port,
    @JsonProperty("amenities") List<String> amenities,
    @JsonProperty("owner") OwnerInfo owner) {

  public record PortInfo(
      @JsonProperty("name") String name,
      @JsonProperty("latitude") double latitude,
      @JsonProperty("longitude") double longitude,
      @JsonProperty("timezone") String timezone) {}

  public record OwnerInfo(
      @JsonProperty("owner_id") String ownerId,
      @JsonProperty("validated_name") String validatedName) {}
}
