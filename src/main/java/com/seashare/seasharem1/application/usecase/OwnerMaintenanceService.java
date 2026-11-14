package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.in.FinishMaintenanceUseCase;
import com.seashare.seasharem1.application.port.in.SendToMaintenanceUseCase;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import java.util.UUID;

/**
 * Mantenimiento manual del propietario. Orden de verificación: existe y es del propietario, el
 * estado lo permite y, por último, se cambia de forma atómica. Pedir un estado en el que la
 * embarcación ya está es un reintento válido y no cambia nada.
 */
public class OwnerMaintenanceService implements SendToMaintenanceUseCase, FinishMaintenanceUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";
  static final String CANNOT_SEND =
      "La embarcación no se puede enviar a mantenimiento en su estado actual.";
  static final String CANNOT_FINISH =
      "El mantenimiento no se puede terminar en el estado actual de la embarcación.";
  static final String ADMIN_SUSPENDED =
      "La embarcación fue suspendida por el administrador; solo él puede habilitarla.";

  private final VesselRepositoryPort vessels;
  private final PortRepositoryPort ports;

  public OwnerMaintenanceService(VesselRepositoryPort vessels, PortRepositoryPort ports) {
    this.vessels = vessels;
    this.ports = ports;
  }

  @Override
  public CompletedBoat sendToMaintenance(UUID ownerId, UUID boatId) {
    Vessel vessel = findOwned(ownerId, boatId);
    if (vessel.status() == VesselStatus.MAINTENANCE) {
      return completed(vessel);
    }
    if (!vessel.canBeSentToMaintenance()) {
      throw conflict(CANNOT_SEND, vessel);
    }
    if (!vessels.startMaintenance(boatId, ownerId, MaintenanceBy.OWNER)) {
      // Otro cambio se adelantó: se relee para responder según el estado nuevo.
      Vessel current = findOwned(ownerId, boatId);
      if (current.status() != VesselStatus.MAINTENANCE) {
        throw conflict(CANNOT_SEND, current);
      }
    }
    return completed(findOwned(ownerId, boatId));
  }

  @Override
  public CompletedBoat finishMaintenance(UUID ownerId, UUID boatId) {
    Vessel vessel = findOwned(ownerId, boatId);
    if (vessel.status() == VesselStatus.AVAILABLE) {
      return completed(vessel);
    }
    if (!vessel.ownerCanFinishMaintenance()) {
      throw conflict(adminSuspended(vessel) ? ADMIN_SUSPENDED : CANNOT_FINISH, vessel);
    }
    if (!vessels.finishMaintenance(boatId, ownerId)) {
      Vessel current = findOwned(ownerId, boatId);
      if (current.status() != VesselStatus.AVAILABLE) {
        throw conflict(adminSuspended(current) ? ADMIN_SUSPENDED : CANNOT_FINISH, current);
      }
    }
    return completed(findOwned(ownerId, boatId));
  }

  private static boolean adminSuspended(Vessel vessel) {
    return vessel.status() == VesselStatus.MAINTENANCE
        && vessel.maintenanceBy() == MaintenanceBy.ADMIN;
  }

  private static BoatMaintenanceConflictException conflict(String message, Vessel vessel) {
    return new BoatMaintenanceConflictException(message, vessel.status(), vessel.maintenanceBy());
  }

  private Vessel findOwned(UUID ownerId, UUID boatId) {
    return vessels
        .findById(boatId)
        .filter(found -> found.ownerId().equals(ownerId))
        .filter(found -> !found.isDraft())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }

  private CompletedBoat completed(Vessel vessel) {
    Port port = vessel.portId() == null ? null : ports.findById(vessel.portId()).orElse(null);
    return new CompletedBoat(vessel, port);
  }
}
