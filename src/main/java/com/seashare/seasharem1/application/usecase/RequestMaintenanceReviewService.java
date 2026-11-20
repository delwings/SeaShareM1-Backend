package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.RequestMaintenanceReviewUseCase;
import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * "Solicitar revisión" del propietario (historia 4). Solo aplica a una embarcación que el
 * administrador envió a mantenimiento; no cambia su estado, solo avisa a cada administrador.
 */
public class RequestMaintenanceReviewService implements RequestMaintenanceReviewUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";
  static final String NOT_ADMIN_MAINTENANCE =
      "Solo se puede solicitar revisión de una embarcación enviada a mantenimiento por el"
          + " administrador.";

  private final VesselRepositoryPort vessels;
  private final UserRepositoryPort users;
  private final NotificationRepositoryPort notifications;
  private final Clock clock;

  public RequestMaintenanceReviewService(
      VesselRepositoryPort vessels,
      UserRepositoryPort users,
      NotificationRepositoryPort notifications,
      Clock clock) {
    this.vessels = vessels;
    this.users = users;
    this.notifications = notifications;
    this.clock = clock;
  }

  @Override
  public void requestReview(UUID ownerId, UUID boatId) {
    Vessel vessel =
        vessels
            .findById(boatId)
            .filter(found -> found.ownerId().equals(ownerId))
            .filter(found -> !found.isDraft() && !found.deleted())
            .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    if (vessel.status() != VesselStatus.MAINTENANCE || !vessel.adminCanFinishMaintenance()) {
      throw new BoatMaintenanceConflictException(
          NOT_ADMIN_MAINTENANCE, vessel.status(), vessel.maintenanceBy());
    }
    if (vessel.reviewRequested() || !vessels.markReviewRequested(vessel.id(), ownerId)) {
      // Ya se había solicitado (o lo ganó otra petición a la vez): no se avisa de nuevo.
      return;
    }
    LocalDateTime now = LocalDateTime.now(clock);
    String message =
        "El propietario solicitó la revisión de la embarcación %s (%s) el %s."
            .formatted(vessel.name(), vessel.legalRegistration(), OwnerNotifier.dateText(now));
    for (User admin : users.findAllByRole(UserRole.ADMIN)) {
      try {
        notifications.save(
            new Notification(
                UUID.randomUUID(),
                admin.id(),
                vessel.id(),
                NotificationType.REVIEW_REQUEST,
                message,
                false,
                now));
      } catch (RuntimeException failure) {
        OwnerNotifier.logFailure(vessel, NotificationType.REVIEW_REQUEST, failure);
      }
    }
  }
}
