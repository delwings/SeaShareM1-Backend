package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Respuesta de GET /api/v1/fleet/boat-types. */
public record BoatTypeListResponse(@JsonProperty("types") List<BoatTypeResponse> types) {}
