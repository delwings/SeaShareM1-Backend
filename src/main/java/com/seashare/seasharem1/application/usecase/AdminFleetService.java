package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.in.FleetFilter;
import com.seashare.seasharem1.application.port.in.FleetItem;
import com.seashare.seasharem1.application.port.in.GetAdminBoatDetailUseCase;
import com.seashare.seasharem1.application.port.in.ListFleetOwnersUseCase;
import com.seashare.seasharem1.application.port.in.ListFleetUseCase;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.Vessel;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consultas globales del Administrador: listado de flota con filtros, propietarios para el filtro y
 * detalle de cualquier embarcación registrada. Solo lectura.
 */
public class AdminFleetService
    implements ListFleetUseCase, ListFleetOwnersUseCase, GetAdminBoatDetailUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";

  private final VesselRepositoryPort vessels;
  private final UserRepositoryPort users;
  private final PortRepositoryPort ports;

  public AdminFleetService(
      VesselRepositoryPort vessels, UserRepositoryPort users, PortRepositoryPort ports) {
    this.vessels = vessels;
    this.users = users;
    this.ports = ports;
  }

  @Override
  public List<FleetItem> list(FleetFilter filter) {
    List<Vessel> registered = vessels.findAllRegistered();
    Map<UUID, User> owners = ownersOf(registered);
    String query = normalize(filter.query());
    return registered.stream()
        .filter(v -> filter.ownerId() == null || v.ownerId().equals(filter.ownerId()))
        .filter(v -> filter.status() == null || v.status() == filter.status())
        .filter(v -> query.isEmpty() || matches(owners.get(v.ownerId()), query))
        .sorted(
            Comparator.comparing(
                    Vessel::registeredAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Vessel::name, String.CASE_INSENSITIVE_ORDER))
        .map(v -> new FleetItem(v, owners.get(v.ownerId())))
        .toList();
  }

  @Override
  public List<User> listOwners() {
    return ownersOf(vessels.findAllRegistered()).values().stream()
        .sorted(Comparator.comparing(User::fullName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Override
  public CompletedBoat getDetail(UUID boatId) {
    Vessel vessel =
        vessels
            .findById(boatId)
            .filter(found -> !found.isDraft())
            .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    Port port = vessel.portId() == null ? null : ports.findById(vessel.portId()).orElse(null);
    return new CompletedBoat(vessel, port);
  }

  private Map<UUID, User> ownersOf(List<Vessel> registered) {
    Set<UUID> ids = registered.stream().map(Vessel::ownerId).collect(Collectors.toSet());
    return users.findByIds(ids).stream().collect(Collectors.toMap(User::id, Function.identity()));
  }

  /** El texto coincide con el nombre del propietario (sin tildes ni mayúsculas) o su cédula. */
  private static boolean matches(User owner, String query) {
    if (owner == null) {
      return false;
    }
    return normalize(owner.fullName()).contains(query)
        || String.valueOf(owner.documentNumber()).contains(query);
  }

  private static String normalize(String text) {
    if (text == null) {
      return "";
    }
    return Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }
}
