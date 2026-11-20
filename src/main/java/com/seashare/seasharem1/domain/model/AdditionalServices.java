package com.seashare.seasharem1.domain.model;

/**
 * Servicios adicionales opcionales de una embarcación. Los servicios base (Capitán y Combustible)
 * van siempre incluidos y no se guardan.
 */
public record AdditionalServices(
    boolean lifeJackets,
    boolean coolerWithIce,
    boolean soundSystem,
    boolean fishingGear,
    boolean divingGear) {

  /** Sin ningún servicio adicional. */
  public static AdditionalServices none() {
    return new AdditionalServices(false, false, false, false, false);
  }
}
