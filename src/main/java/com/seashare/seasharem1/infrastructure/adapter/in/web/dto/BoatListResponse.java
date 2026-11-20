package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta de GET /api/v1/fleet/boats. */
public record BoatListResponse(@JsonProperty("boats") List<BoatListItemResponse> boats) {}
