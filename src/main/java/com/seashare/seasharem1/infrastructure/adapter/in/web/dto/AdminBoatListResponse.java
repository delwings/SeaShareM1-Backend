package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

/** Listado "Control General de Flota Fluvial". */
public record AdminBoatListResponse(@JsonProperty("boats") List<Item> boats) {

  /** Una fila: la embarcación con su propietario. */
  public record Item(
      @JsonProperty("boat_id") UUID boatId,
      @JsonProperty("name") String name,
      @JsonProperty("owner") OwnerResponse owner,
      @JsonProperty("legal_registration") String legalRegistration,
      @JsonProperty("type") String type,
      @JsonProperty("status") String status) {}
}
