package com.seashare.seasharem1.domain.model;

/** Tipo de embarcación y su capacidad máxima de pasajeros. */
public enum VesselType {
  MOTORBOAT(10), // Lancha
  YACHT(20), // Yate
  CATAMARAN(30), // Catamarán
  SAILBOAT(12); // Velero

  private final int maxCapacity;

  VesselType(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  /** Capacidad máxima de pasajeros que admite este tipo. */
  public int maxCapacity() {
    return maxCapacity;
  }

  /** Indica si una capacidad es válida para este tipo: al menos 1 y no más que el máximo. */
  public boolean allows(int capacity) {
    return capacity >= 1 && capacity <= maxCapacity;
  }
}
