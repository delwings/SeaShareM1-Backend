package com.seashare.seasharem1.domain.exception;

import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.VesselStatus;

/**
 * La embarcación existe y es del propietario, pero su estado actual no permite el cambio de
 * mantenimiento pedido (Reservado, En Navegación, o suspendida por el administrador). Es un 409 que
 * informa el estado actual y, si está en mantenimiento, quién la envió.
 */
public class BoatMaintenanceConflictException extends BusinessConflictException {

  private final VesselStatus currentStatus;
  private final MaintenanceBy maintenanceBy;

  public BoatMaintenanceConflictException(
      String message, VesselStatus currentStatus, MaintenanceBy maintenanceBy) {
    super(message);
    this.currentStatus = currentStatus;
    this.maintenanceBy = maintenanceBy;
  }

  public VesselStatus currentStatus() {
    return currentStatus;
  }

  /** Quién envió la embarcación a mantenimiento; nulo si no está en mantenimiento. */
  public MaintenanceBy maintenanceBy() {
    return maintenanceBy;
  }
}
