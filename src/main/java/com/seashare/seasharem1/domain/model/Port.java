package com.seashare.seasharem1.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** Puerto de atraque, con su nombre (único) y coordenadas. */
public record Port(UUID id, String name, BigDecimal latitude, BigDecimal longitude) {

  /** Indica si las coordenadas son las mismas (compara el valor, no la escala). */
  public boolean sameLocationAs(BigDecimal otherLatitude, BigDecimal otherLongitude) {
    return latitude.compareTo(otherLatitude) == 0 && longitude.compareTo(otherLongitude) == 0;
  }
}
