package com.seashare.seasharem1.domain.exception;

import com.seashare.seasharem1.domain.model.VesselStatus;

/**
 * La embarcación existe y es del propietario, pero su estado actual no permite eliminarla
 * (Reservado o En Navegación). Es un 409 que además informa el estado actual, para que la pantalla
 * pueda decir en cuál quedó.
 */
public class BoatNotDeletableException extends BusinessConflictException {

  private final VesselStatus currentStatus;

  public BoatNotDeletableException(VesselStatus currentStatus) {
    super("La embarcación no se puede eliminar en su estado actual.");
    this.currentStatus = currentStatus;
  }

  public VesselStatus currentStatus() {
    return currentStatus;
  }
}
