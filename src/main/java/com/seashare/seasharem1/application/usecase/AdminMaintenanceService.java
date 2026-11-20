package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.AdminMaintenanceUseCase;
import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.MaintenanceWindow;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mantenimiento del administrador. Orden: motivo válido, existe, el estado lo permite y se cambia
 * de forma atómica; solo entonces se avisa al propietario (un fallo del aviso no revierte nada, y
 * un reintento idempotente no avisa dos veces).
 */
public class AdminMaintenanceService implements AdminMaintenanceUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";
  static final String CANNOT_SEND =
      "La embarcación no se puede enviar a mantenimiento en su estado actual.";
  static final String CANNOT_FINISH =
      "El mantenimiento no se puede terminar en el estado actual de la embarcación.";
  static final String OWNER_STARTED =
      "El mantenimiento lo inició el propietario; solo él puede terminarlo.";

  private final VesselRepositoryPort vessels;
  private final PortRepositoryPort ports;
  private final OwnerNotifier notifier;
  private final Clock clock;

  public AdminMaintenanceService(
      VesselRepositoryPort vessels,
      PortRepositoryPort ports,
      NotificationRepositoryPort notifications,
      Clock clock) {
    this.vessels = vessels;
    this.ports = ports;
    this.notifier = new OwnerNotifier(notifications);
    this.clock = clock;
  }

  @Override
  public CompletedBoat sendToMaintenance(
      UUID adminId, UUID boatId, String reason, Instant start, Instant until) {
    String cleanReason = AdminReason.require(reason, "Escribe el motivo del mantenimiento.");
    Vessel vessel = find(boatId);
    MaintenanceWindow window = MaintenancePeriod.resolve(start, until, clock);
    if (vessel.status() == VesselStatus.MAINTENANCE) {
      return completed(vessel);
    }
    if (!vessel.canBeSentToMaintenance()) {
      throw conflict(CANNOT_SEND, vessel);
    }
    if (!vessels.startMaintenanceAsAdmin(boatId, window)) {
      Vessel current = find(boatId);
      if (current.status() != VesselStatus.MAINTENANCE) {
        throw conflict(CANNOT_SEND, current);
      }
      return completed(current);
    }
    LocalDateTime now = LocalDateTime.now(clock);
    notifier.notify(
        vessel,
        NotificationType.MAINTENANCE_NOTICE,
        "Tu embarcación %s (%s) fue enviada a mantenimiento por un administrador el %s. Motivo: %s"
            .formatted(
                vessel.name(),
                vessel.legalRegistration(),
                OwnerNotifier.dateText(now),
                cleanReason),
        now);
    return completed(find(boatId));
  }

  @Override
  public CompletedBoat finishMaintenance(UUID adminId, UUID boatId) {
    Vessel vessel = find(boatId);
    if (vessel.status() == VesselStatus.AVAILABLE) {
      return completed(vessel);
    }
    if (!vessel.adminCanFinishMaintenance()) {
      throw conflict(ownerStarted(vessel) ? OWNER_STARTED : CANNOT_FINISH, vessel);
    }
    if (!vessels.finishMaintenanceAsAdmin(boatId)) {
      Vessel current = find(boatId);
      if (current.status() != VesselStatus.AVAILABLE) {
        throw conflict(ownerStarted(current) ? OWNER_STARTED : CANNOT_FINISH, current);
      }
      return completed(current);
    }
    LocalDateTime now = LocalDateTime.now(clock);
    notifier.notify(
        vessel,
        NotificationType.VESSEL_AVAILABLE,
        "Tu embarcación %s (%s) fue habilitada por un administrador el %s. Ya puede recibir reservas."
            .formatted(vessel.name(), vessel.legalRegistration(), OwnerNotifier.dateText(now)),
        now);
    return completed(find(boatId));
  }

  /**
   * En mantenimiento por el propietario o por el sistema (origen vacío): no es del administrador.
   */
  private static boolean ownerStarted(Vessel vessel) {
    return vessel.status() == VesselStatus.MAINTENANCE && !vessel.adminCanFinishMaintenance();
  }

  private static BoatMaintenanceConflictException conflict(String message, Vessel vessel) {
    return new BoatMaintenanceConflictException(message, vessel.status(), vessel.maintenanceBy());
  }

  private Vessel find(UUID boatId) {
    return vessels
        .findById(boatId)
        .filter(found -> !found.isDraft())
        .filter(found -> !found.deleted())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }

  private CompletedBoat completed(Vessel vessel) {
    Port port = vessel.portId() == null ? null : ports.findById(vessel.portId()).orElse(null);
    return new CompletedBoat(vessel, port);
  }
}
