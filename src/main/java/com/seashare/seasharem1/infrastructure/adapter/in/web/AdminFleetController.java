package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.AdminDeleteBoatUseCase;
import com.seashare.seasharem1.application.port.in.AdminMaintenanceUseCase;
import com.seashare.seasharem1.application.port.in.FleetFilter;
import com.seashare.seasharem1.application.port.in.GetAdminBoatDetailUseCase;
import com.seashare.seasharem1.application.port.in.ListFleetOwnersUseCase;
import com.seashare.seasharem1.application.port.in.ListFleetUseCase;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.AdminBoatListResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.AdminDeleteRequest;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.CompletedBoatResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.MaintenanceRequest;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OwnerListResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OwnerResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Control general de flota del Administrador: listado global con filtros, propietarios para el
 * filtro y detalle de cualquier embarcación. Solo el rol ADMIN entra; todo es de lectura.
 */
@RestController
@RequestMapping("/api/v1/admin/fleet")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFleetController {

  private static final Set<VesselStatus> FILTERABLE =
      Set.of(
          VesselStatus.AVAILABLE,
          VesselStatus.RESERVED,
          VesselStatus.MAINTENANCE,
          VesselStatus.NAVIGATION);
  private static final int MAX_QUERY_LENGTH = 100;

  private final ListFleetUseCase listFleet;
  private final ListFleetOwnersUseCase listOwners;
  private final GetAdminBoatDetailUseCase getDetail;
  private final AdminDeleteBoatUseCase deleteBoat;
  private final AdminMaintenanceUseCase maintenance;

  public AdminFleetController(
      ListFleetUseCase listFleet,
      ListFleetOwnersUseCase listOwners,
      GetAdminBoatDetailUseCase getDetail,
      AdminDeleteBoatUseCase deleteBoat,
      AdminMaintenanceUseCase maintenance) {
    this.listFleet = listFleet;
    this.listOwners = listOwners;
    this.getDetail = getDetail;
    this.deleteBoat = deleteBoat;
    this.maintenance = maintenance;
  }

  /** Listado global; los tres filtros son opcionales y se combinan. */
  @GetMapping("/boats")
  public AdminBoatListResponse list(
      @RequestParam(value = "owner_id", required = false) String ownerId,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "q", required = false) String query) {
    FleetFilter filter = parseFilter(ownerId, status, query);
    return new AdminBoatListResponse(
        listFleet.list(filter).stream()
            .map(
                item ->
                    new AdminBoatListResponse.Item(
                        item.vessel().id(),
                        item.vessel().name(),
                        toOwner(item.owner()),
                        item.vessel().legalRegistration(),
                        item.vessel().type().name(),
                        item.vessel().status().name()))
            .toList());
  }

  /** Propietarios con embarcaciones, para el filtro "Filtrar por Propietario". */
  @GetMapping("/owners")
  public OwnerListResponse owners() {
    return new OwnerListResponse(listOwners.listOwners().stream().map(this::toOwner).toList());
  }

  /** Detalle de cualquier embarcación registrada; 404 si no existe, fue eliminada o es borrador. */
  @GetMapping("/boats/{boatId}")
  public CompletedBoatResponse detail(@PathVariable UUID boatId) {
    return BoatWebMapper.toCompleted(getDetail.getDetail(boatId));
  }

  /**
   * Eliminación lógica de cualquier embarcación registrada; el cuerpo trae el motivo (obligatorio,
   * 400 si falta) y se avisa al propietario con él. 404 si no existe, ya fue eliminada o es
   * borrador; 409 por estado.
   */
  @DeleteMapping("/boats/{boatId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID boatId,
      @RequestBody(required = false) AdminDeleteRequest request) {
    deleteBoat.deleteBoat(
        UUID.fromString(jwt.getSubject()), boatId, request == null ? null : request.reason());
  }

  /** Envía a mantenimiento con motivo obligatorio (400 si falta) y avisa al propietario. */
  @PostMapping("/boats/{boatId}/maintenance/start")
  public CompletedBoatResponse startMaintenance(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID boatId,
      @RequestBody(required = false) MaintenanceRequest request) {
    return BoatWebMapper.toCompleted(
        maintenance.sendToMaintenance(
            UUID.fromString(jwt.getSubject()),
            boatId,
            request == null ? null : request.reason(),
            MaintenanceDates.parse(
                "maintenance_start", request == null ? null : request.maintenanceStart()),
            MaintenanceDates.parse(
                "maintenance_until", request == null ? null : request.maintenanceUntil())));
  }

  /** Termina el mantenimiento que inició el administrador; el del propietario da 409. */
  @PostMapping("/boats/{boatId}/maintenance/finish")
  public CompletedBoatResponse finishMaintenance(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID boatId) {
    return BoatWebMapper.toCompleted(
        maintenance.finishMaintenance(UUID.fromString(jwt.getSubject()), boatId));
  }

  private OwnerResponse toOwner(User owner) {
    return owner == null
        ? null
        : new OwnerResponse(owner.id(), owner.fullName(), String.valueOf(owner.documentNumber()));
  }

  private static FleetFilter parseFilter(String ownerId, String status, String query) {
    List<FieldViolation> errors = new ArrayList<>();
    UUID owner = null;
    VesselStatus parsedStatus = null;
    if (ownerId != null && !ownerId.isBlank()) {
      try {
        owner = UUID.fromString(ownerId.trim());
      } catch (IllegalArgumentException invalid) {
        errors.add(new FieldViolation("owner_id", "El propietario no es válido"));
      }
    }
    if (status != null && !status.isBlank()) {
      try {
        parsedStatus = VesselStatus.valueOf(status.trim());
        if (!FILTERABLE.contains(parsedStatus)) {
          throw new IllegalArgumentException();
        }
      } catch (IllegalArgumentException invalid) {
        parsedStatus = null;
        errors.add(new FieldViolation("status", "El estado no es válido"));
      }
    }
    String text = query == null ? null : query.trim();
    if (text != null && text.length() > MAX_QUERY_LENGTH) {
      errors.add(
          new FieldViolation(
              "q", "La búsqueda no puede superar " + MAX_QUERY_LENGTH + " caracteres"));
    }
    if (!errors.isEmpty()) {
      throw new ValidationFailedException(errors);
    }
    return new FleetFilter(owner, parsedStatus, text);
  }
}
