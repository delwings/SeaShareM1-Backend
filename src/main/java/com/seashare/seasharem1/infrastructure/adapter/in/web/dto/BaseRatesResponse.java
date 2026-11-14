package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Tarifas encontradas y, aparte, los identificadores que no se encontraron. */
public record BaseRatesResponse(
    @JsonProperty("rates") List<Rate> rates, @JsonProperty("not_found") List<String> notFound) {

  /** Tarifa base como texto con dos decimales, p. ej. "350000.00". */
  public record Rate(
      @JsonProperty("boat_id") String boatId, @JsonProperty("base_rate") String baseRate) {}
}
