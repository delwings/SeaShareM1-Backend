package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Tipo de embarcación con su capacidad máxima, para llenar el selector. */
public record BoatTypeResponse(
    @JsonProperty("code") String code,
    @JsonProperty("name") String name,
    @JsonProperty("max_capacity") int maxCapacity) {}
