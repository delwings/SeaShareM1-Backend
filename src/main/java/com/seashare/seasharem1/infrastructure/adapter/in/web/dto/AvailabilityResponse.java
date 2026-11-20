package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Respuesta de la verificación de matrícula: true si está libre. */
public record AvailabilityResponse(@JsonProperty("available") boolean available) {}
