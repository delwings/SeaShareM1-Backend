package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase;
import com.seashare.seasharem1.application.port.out.ProcessedEventRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Aplica los avisos de estado operativo del Módulo 2 sin repetirlos ni retroceder el tiempo. */
public class AssignOperationalStatusService implements AssignOperationalStatusUseCase {

  static final int MAX_EVENT_ID = 100;

  /** Transiciones que M2 puede ordenar (escenarios del spec); cualquier otra se ignora. */
  private static final Map<VesselStatus, Set<VesselStatus>> ALLOWED =
      new EnumMap<>(VesselStatus.class);

  static {
    // AVAILABLE -> NAVIGATION: el Módulo 2 ya no envía RESERVED (contrato v2.0, D-03).
    ALLOWED.put(VesselStatus.AVAILABLE, EnumSet.of(VesselStatus.RESERVED, VesselStatus.NAVIGATION));
    ALLOWED.put(
        VesselStatus.RESERVED,
        EnumSet.of(VesselStatus.NAVIGATION, VesselStatus.MAINTENANCE, VesselStatus.AVAILABLE));
    ALLOWED.put(VesselStatus.NAVIGATION, EnumSet.of(VesselStatus.MAINTENANCE));
  }

  private final VesselRepositoryPort vessels;
  private final ProcessedEventRepositoryPort processedEvents;
  private final Clock clock;

  public AssignOperationalStatusService(
      VesselRepositoryPort vessels, ProcessedEventRepositoryPort processedEvents, Clock clock) {
    this.vessels = vessels;
    this.processedEvents = processedEvents;
    this.clock = clock;
  }

  @Override
  public Outcome assign(StatusEvent event) {
    ParsedEvent parsed = parse(event);

    if (processedEvents.exists(parsed.eventId())) {
      return Outcome.DUPLICATE;
    }
    Outcome outcome = decide(parsed);
    processedEvents.register(parsed.eventId(), parsed.vesselId(), LocalDateTime.now(clock));
    return outcome;
  }

  private Outcome decide(ParsedEvent event) {
    Optional<Vessel> found =
        vessels.findById(event.vesselId()).filter(v -> !v.isDraft() && !v.deleted());
    if (found.isEmpty()) {
      return Outcome.UNKNOWN_VESSEL;
    }
    VesselStatus current = found.get().status();

    Optional<LocalDateTime> lastChange = vessels.findStatusChangedAt(event.vesselId());
    if (lastChange.isPresent() && event.occurredAt().isBefore(lastChange.get())) {
      return Outcome.STALE;
    }
    if (current == event.status()) {
      return Outcome.NO_CHANGE;
    }
    if (!ALLOWED.getOrDefault(current, Set.of()).contains(event.status())) {
      return Outcome.INVALID_TRANSITION;
    }
    boolean changed =
        vessels.changeStatusAutomatically(
            event.vesselId(), current, event.status(), event.occurredAt(), event.eventId());
    // Si no cambió, otro aviso o una acción manual se adelantó entre la lectura y la escritura.
    return changed ? Outcome.APPLIED : Outcome.STALE;
  }

  private static ParsedEvent parse(StatusEvent event) {
    List<FieldViolation> violations = new ArrayList<>();
    String eventId = event == null || event.eventId() == null ? "" : event.eventId().trim();
    if (eventId.isEmpty() || eventId.length() > MAX_EVENT_ID) {
      violations.add(
          new FieldViolation("event_id", "El identificador del aviso es obligatorio (máx. 100)."));
    }
    UUID vesselId = null;
    try {
      vesselId = UUID.fromString(event == null ? "" : String.valueOf(event.vesselId()).trim());
    } catch (IllegalArgumentException e) {
      violations.add(new FieldViolation("vessel_id", "La embarcación no es válida."));
    }
    VesselStatus status = event == null ? null : toStatus(event.operationalStatus());
    if (status == null) {
      violations.add(new FieldViolation("operational_status", "El estado operativo no es válido."));
    }
    LocalDateTime occurredAt = null;
    try {
      occurredAt =
          OffsetDateTime.parse(String.valueOf(event == null ? null : event.occurredAt()).trim())
              .withOffsetSameInstant(ZoneOffset.UTC)
              .toLocalDateTime();
    } catch (RuntimeException e) {
      violations.add(
          new FieldViolation("occurred_at", "La fecha y hora del suceso no son válidas."));
    }
    if (!violations.isEmpty()) {
      throw new ValidationFailedException(violations);
    }
    return new ParsedEvent(eventId, vesselId, status, occurredAt);
  }

  /** Acepta los nombres de M2 (IN_NAVIGATION, MAINTENANCE_CLEANING) y los propios del módulo. */
  private static VesselStatus toStatus(String value) {
    if (value == null) {
      return null;
    }
    return switch (value.trim().toUpperCase()) {
      case "AVAILABLE" -> VesselStatus.AVAILABLE;
      case "RESERVED" -> VesselStatus.RESERVED;
      case "IN_NAVIGATION", "NAVIGATION" -> VesselStatus.NAVIGATION;
      case "MAINTENANCE_CLEANING", "MAINTENANCE" -> VesselStatus.MAINTENANCE;
      default -> null;
    };
  }

  private record ParsedEvent(
      String eventId, UUID vesselId, VesselStatus status, LocalDateTime occurredAt) {}
}
