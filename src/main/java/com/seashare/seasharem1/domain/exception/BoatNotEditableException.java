package com.seashare.seasharem1.domain.exception;

import com.seashare.seasharem1.domain.model.VesselStatus;

/**
 * La embarcación existe y es del propietario, pero su estado actual no permite editarla (Reservado
 * o En Navegación). Es un 409 que informa el estado actual para que la pantalla diga en cuál quedó.
 */
public class BoatNotEditableException extends BusinessConflictException {

  private final VesselStatus currentStatus;

  public BoatNotEditableException(VesselStatus currentStatus) {
    super("La embarcación no se puede editar en su estado actual.");
    this.currentStatus = currentStatus;
  }

  public VesselStatus currentStatus() {
    return currentStatus;
  }
}
