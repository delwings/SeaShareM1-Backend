package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Catálogo de embarcaciones para el Módulo 2 (contrato v2.0, C2). */
public record VesselCatalogResponse(
    @JsonProperty("vessels") List<Item> vessels,
    @JsonProperty("pagination") Pagination pagination) {

  public record Item(
      @JsonProperty("vessel_id") String vesselId,
      @JsonProperty("name") String name,
      @JsonProperty("type") String type,
      @JsonProperty("photo_url") String photoUrl,
      @JsonProperty("port") VesselInfoResponse.PortInfo port,
      @JsonProperty("max_capacity") int maxCapacity,
      @JsonProperty("captain_included") boolean captainIncluded) {}

  public record Pagination(
      @JsonProperty("page") int page,
      @JsonProperty("page_size") int pageSize,
      @JsonProperty("total_items") long totalItems,
      @JsonProperty("total_pages") int totalPages) {}
}
