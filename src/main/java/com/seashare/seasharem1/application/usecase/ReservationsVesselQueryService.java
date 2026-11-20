package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselPage;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.MaintenanceWindow;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Consultas de solo lectura para el Módulo 2: leen siempre el dato guardado en ese momento. */
public class ReservationsVesselQueryService implements ReservationsVesselQuery {

  static final String NOT_FOUND = "Embarcación no encontrada.";
  static final int DEFAULT_PAGE_SIZE = 20;
  static final int MAX_PAGE_SIZE = 100;

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
        effectiveStatus(vessel),
        vessel.photoPath(),
        port,
        amenities(vessel),
        vessel.ownerId(),
        ownerName);
  }

  @Override
  public OperationalStatus getOperationalStatus(UUID vesselId) {
    Vessel vessel = registered(vesselId);
    MaintenanceWindow window = activeWindow(vessel);
    return new OperationalStatus(
        vessel.id(),
        effectiveStatus(vessel),
        clock.instant(),
        window == null ? null : window.start().toInstant(ZoneOffset.UTC),
        window == null ? null : window.until().toInstant(ZoneOffset.UTC));
  }

  @Override
  public CatalogPage getCatalog(String query, int page, int size) {
    int safePage = Math.max(page, 1);
    int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
    VesselPage found = vessels.searchRegistered(query, safePage, safeSize);
    Map<UUID, Port> portsById = new HashMap<>();
    List<CatalogItem> items =
        found.items().stream()
            .map(
                vessel -> {
                  Port port =
                      vessel.portId() == null
                          ? null
                          : portsById.computeIfAbsent(
                              vessel.portId(), id -> ports.findById(id).orElse(null));
                  return new CatalogItem(
                      vessel.id(),
                      vessel.name(),
                      vessel.type(),
                      vessel.photoPath(),
                      port,
                      vessel.maxCapacity());
                })
            .toList();
    return new CatalogPage(items, safePage, safeSize, found.totalItems());
  }

  /**
   * Liberación perezosa (contrato v2.0, D-07): si el fin del mantenimiento ya pasó se informa
   * Disponible aunque lo guardado siga en mantenimiento. No se modifica nada en la base.
   */
  private VesselStatus effectiveStatus(Vessel vessel) {
    if (vessel.status() == VesselStatus.MAINTENANCE && maintenanceExpired(vessel)) {
      return VesselStatus.AVAILABLE;
    }
    return vessel.status();
  }

  private MaintenanceWindow activeWindow(Vessel vessel) {
    if (vessel.status() != VesselStatus.MAINTENANCE) {
      return null;
    }
    return vessels
        .findMaintenanceWindow(vessel.id())
        .filter(window -> !window.expiredAt(LocalDateTime.now(clock)))
        .orElse(null);
  }

  private boolean maintenanceExpired(Vessel vessel) {
    return vessels
        .findMaintenanceWindow(vessel.id())
        .map(window -> window.expiredAt(LocalDateTime.now(clock)))
        .orElse(false);
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
