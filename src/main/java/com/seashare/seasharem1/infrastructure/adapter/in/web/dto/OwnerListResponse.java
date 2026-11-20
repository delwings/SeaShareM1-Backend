package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Propietarios con embarcaciones registradas (filtro del Administrador). */
public record OwnerListResponse(@JsonProperty("owners") List<OwnerResponse> owners) {}
