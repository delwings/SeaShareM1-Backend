package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Cuerpo de la consulta de tarifa base: el lote de identificadores de embarcaciones. */
public record BaseRatesRequest(@JsonProperty("boat_ids") List<String> boatIds) {}
