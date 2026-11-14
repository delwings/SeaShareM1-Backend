package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Resultado de buscar propietarios: hasta 5 y si había más coincidencias. */
public record OwnerSearchResponse(
    @JsonProperty("owners") List<OwnerResponse> owners,
    @JsonProperty("has_more") boolean hasMore) {}
