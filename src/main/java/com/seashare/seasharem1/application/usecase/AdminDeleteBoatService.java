package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.AdminDeleteBoatUseCase;
import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BoatNotDeletableException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.Vessel;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Eliminación lógica por el administrador (FR-022): la embarcación existe, el estado lo permite y
 * se marca de forma atómica; solo entonces se avisa al propietario. Un fallo del aviso no revierte
 * la eliminación (FR-020).
 */
public class AdminDeleteBoatService implements AdminDeleteBoatUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";

  private final VesselRepositoryPort vessels;
  private final OwnerNotifier notifier;
  private final Clock clock;

  public AdminDeleteBoatService(
      VesselRepositoryPort vessels, NotificationRepositoryPort notifications, Clock clock) {
    this.vessels = vessels;
    this.notifier = new OwnerNotifier(notifications);
    this.clock = clock;
  }

  @Override
  public void deleteBoat(UUID adminId, UUID boatId, String reason) {
    String cleanReason = AdminReason.require(reason, "Escribe el motivo de la eliminación.");
    Vessel vessel = find(boatId);
    if (!vessel.canBeDeleted()) {
      throw new BoatNotDeletableException(vessel.status());
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (!vessels.markDeleted(boatId, adminId, now)) {
      // Otro cambio se adelantó: se relee para dar el error correcto (404 si ya la eliminaron,
      // 409 con el estado nuevo si cambió). Así nunca se notifica dos veces.
      throw new BoatNotDeletableException(find(boatId).status());
    }
    notifyOwner(vessel, cleanReason, now);
  }

  private Vessel find(UUID boatId) {
    return vessels
        .findById(boatId)
        .filter(found -> !found.isDraft())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }

  private void notifyOwner(Vessel vessel, String reason, LocalDateTime now) {
    String message =
        "Tu embarcación %s (%s) fue eliminada por un administrador el %s. Motivo: %s"
            .formatted(
                vessel.name(), vessel.legalRegistration(), OwnerNotifier.dateText(now), reason);
    notifier.notify(vessel, NotificationType.VESSEL_DELETED, message, now);
  }
}
