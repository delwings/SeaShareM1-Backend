package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.DeleteBoatUseCase;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BoatNotDeletableException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Vessel;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Eliminación lógica de una embarcación. Orden de verificación (FR-022): existe y es del
 * propietario, el estado lo permite y, por último, se marca de forma atómica.
 */
public class DeleteBoatService implements DeleteBoatUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";

  private final VesselRepositoryPort vessels;
  private final Clock clock;

  public DeleteBoatService(VesselRepositoryPort vessels, Clock clock) {
    this.vessels = vessels;
    this.clock = clock;
  }

  @Override
  public void deleteBoat(UUID ownerId, UUID boatId) {
    Vessel vessel = findOwned(ownerId, boatId);
    if (!vessel.canBeDeleted()) {
      throw new BoatNotDeletableException(vessel.status());
    }
    if (!vessels.markDeleted(boatId, ownerId, LocalDateTime.now(clock))) {
      // Otro cambio se adelantó entre la lectura y la marca: se relee para dar el error correcto
      // (si la eliminaron, findOwned lanza 404; si cambió de estado, 409 con el estado nuevo).
      throw new BoatNotDeletableException(findOwned(ownerId, boatId).status());
    }
  }

  private Vessel findOwned(UUID ownerId, UUID boatId) {
    return vessels
        .findById(boatId)
        .filter(found -> found.ownerId().equals(ownerId))
        .filter(found -> !found.isDraft())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }
}
