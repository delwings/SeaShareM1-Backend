package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.Vessel;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Consultas de solo lectura para el Módulo 2: leen siempre el dato guardado en ese momento. */
public class ReservationsVesselQueryService implements ReservationsVesselQuery {

  static final String NOT_FOUND = "Embarcación no encontrada.";

  private final VesselRepositoryPort vessels;
  private final PortRepositoryPort ports;
  private final UserRepositoryPort users;
  private final Clock clock;

  public ReservationsVesselQueryService(
      VesselRepositoryPort vessels,
      PortRepositoryPort ports,
      UserRepositoryPort users,
      Clock clock) {
    this.vessels = vessels;
    this.ports = ports;
    this.users = users;
    this.clock = clock;
  }

  @Override
  public VesselInfo getInfo(UUID vesselId) {
    Vessel vessel = registered(vesselId);
    Port port = vessel.portId() == null ? null : ports.findById(vessel.portId()).orElse(null);
    String ownerName = users.findById(vessel.ownerId()).map(User::fullName).orElse(null);
    return new VesselInfo(
        vessel.id(),
        vessel.name(),
        vessel.legalRegistration(),
        vessel.type(),
        vessel.maxCapacity(),
        vessel.status(),
        vessel.photoPath(),
        port,
        amenities(vessel),
        vessel.ownerId(),
        ownerName);
  }

  @Override
  public OperationalStatus getOperationalStatus(UUID vesselId) {
    Vessel vessel = registered(vesselId);
    return new OperationalStatus(vessel.id(), vessel.status(), clock.instant());
  }

  private Vessel registered(UUID vesselId) {
    return vessels
        .findById(vesselId)
        .filter(found -> !found.isDraft() && !found.deleted())
        .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
  }

  /** Servicios base (siempre incluidos) y los adicionales que el propietario seleccionó. */
  private static List<String> amenities(Vessel vessel) {
    List<String> out = new ArrayList<>(List.of("Capitán", "Combustible"));
    if (Boolean.TRUE.equals(vessel.hasLifeJackets())) out.add("Chalecos salvavidas");
    if (Boolean.TRUE.equals(vessel.hasCoolerWithIce())) out.add("Nevera con hielo");
    if (Boolean.TRUE.equals(vessel.hasSoundSystem())) out.add("Equipo de sonido");
    if (Boolean.TRUE.equals(vessel.hasFishingGear())) out.add("Equipo de pesca");
    if (Boolean.TRUE.equals(vessel.hasDivingGear())) out.add("Equipo de buceo");
    return out;
  }
}
