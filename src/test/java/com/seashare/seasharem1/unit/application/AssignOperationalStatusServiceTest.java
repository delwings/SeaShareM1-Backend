package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase.Outcome;
import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase.StatusEvent;
import com.seashare.seasharem1.application.usecase.AssignOperationalStatusService;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssignOperationalStatusServiceTest {

  private Fakes.Vessels vessels;
  private Fakes.ProcessedEvents events;
  private AssignOperationalStatusService service;
  private int sequence;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    events = new Fakes.ProcessedEvents();
    service =
        new AssignOperationalStatusService(
            vessels, events, Clock.fixed(Instant.parse("2026-11-13T20:00:00Z"), ZoneOffset.UTC));
  }

  private Vessel boat(VesselStatus status) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Barco",
                "CP-12-" + (900 + sequence++) + "-B",
                VesselType.YACHT,
                10));
    Vessel done =
        vessels.save(
            draft.complete(
                "https://img.test/a.png",
                AdditionalServices.none(),
                new BigDecimal("100000"),
                UUID.randomUUID(),
                LocalDateTime.of(2026, 11, 1, 10, 0)));
    return vessels.save(
        new Vessel(
            done.id(),
            done.name(),
            done.legalRegistration(),
            done.type(),
            done.maxCapacity(),
            done.photoPath(),
            done.hasLifeJackets(),
            done.hasCoolerWithIce(),
            done.hasSoundSystem(),
            done.hasFishingGear(),
            done.hasDivingGear(),
            done.baseRate(),
            status,
            status == VesselStatus.MAINTENANCE ? MaintenanceBy.OWNER : null,
            done.portId(),
            done.ownerId(),
            false,
            done.registeredAt()));
  }

  private static StatusEvent event(String id, Vessel boat, String status, String at) {
    return new StatusEvent(id, boat.id().toString(), status, at);
  }

  private VesselStatus statusOf(Vessel boat) {
    return vessels.store.get(boat.id()).status();
  }

  @Test
  void aPaidReservationBlocksAnAvailableBoat() {
    Vessel b = boat(VesselStatus.AVAILABLE);

    assertEquals(
        Outcome.APPLIED, service.assign(event("e1", b, "RESERVED", "2026-11-13T10:00:00-05:00")));

    assertEquals(VesselStatus.RESERVED, statusOf(b));
    assertEquals(1, vessels.history.size());
  }

  @Test
  void checkInMovesAReservedBoatToNavigationUsingM2Names() {
    Vessel b = boat(VesselStatus.RESERVED);

    assertEquals(
        Outcome.APPLIED, service.assign(event("e1", b, "IN_NAVIGATION", "2026-11-13T10:00:00Z")));

    assertEquals(VesselStatus.NAVIGATION, statusOf(b));
  }

  @Test
  void aFinishedTripLeavesTheBoatInMaintenanceWithoutOrigin() {
    Vessel b = boat(VesselStatus.NAVIGATION);

    assertEquals(
        Outcome.APPLIED,
        service.assign(event("e1", b, "MAINTENANCE_CLEANING", "2026-11-13T10:00:00Z")));

    assertEquals(VesselStatus.MAINTENANCE, statusOf(b));
    assertEquals(null, vessels.store.get(b.id()).maintenanceBy());
  }

  @Test
  void aDamageCancelationOfAReservedBoatAlsoGoesToMaintenance() {
    Vessel b = boat(VesselStatus.RESERVED);

    assertEquals(
        Outcome.APPLIED, service.assign(event("e1", b, "MAINTENANCE", "2026-11-13T10:00:00Z")));
  }

  @Test
  void anOrdinaryCancelationReleasesAReservedBoat() {
    Vessel b = boat(VesselStatus.RESERVED);

    assertEquals(
        Outcome.APPLIED, service.assign(event("e1", b, "AVAILABLE", "2026-11-13T10:00:00Z")));

    assertEquals(VesselStatus.AVAILABLE, statusOf(b));
  }

  @Test
  void aRepeatedEventIdIsIgnored() {
    Vessel b = boat(VesselStatus.AVAILABLE);
    service.assign(event("e1", b, "RESERVED", "2026-11-13T10:00:00Z"));

    assertEquals(
        Outcome.DUPLICATE, service.assign(event("e1", b, "RESERVED", "2026-11-13T10:00:00Z")));
    assertEquals(1, vessels.history.size());
  }

  @Test
  void anOlderEventThanTheLastChangeIsDiscarded() {
    Vessel b = boat(VesselStatus.AVAILABLE);
    service.assign(event("e1", b, "RESERVED", "2026-11-13T20:00:00Z"));

    assertEquals(
        Outcome.STALE, service.assign(event("e2", b, "AVAILABLE", "2026-11-13T19:00:00Z")));

    assertEquals(VesselStatus.RESERVED, statusOf(b));
  }

  @Test
  void unknownDeletedAndDraftBoatsAreIgnored() {
    Vessel deleted = boat(VesselStatus.AVAILABLE);
    vessels.store.put(deleted.id(), deleted.asDeleted());
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), UUID.randomUUID(), "B", "CP-12-999-B", VesselType.YACHT, 5));

    assertEquals(
        Outcome.UNKNOWN_VESSEL,
        service.assign(event("e1", deleted, "RESERVED", "2026-11-13T10:00:00Z")));
    assertEquals(
        Outcome.UNKNOWN_VESSEL,
        service.assign(event("e2", draft, "RESERVED", "2026-11-13T10:00:00Z")));
    assertEquals(
        Outcome.UNKNOWN_VESSEL,
        service.assign(
            new StatusEvent(
                "e3", UUID.randomUUID().toString(), "RESERVED", "2026-11-13T10:00:00Z")));
  }

  @Test
  void transitionsOutsideTheSpecAreIgnored() {
    Vessel maintenance = boat(VesselStatus.MAINTENANCE);
    Vessel available = boat(VesselStatus.AVAILABLE);

    assertEquals(
        Outcome.INVALID_TRANSITION,
        service.assign(event("e1", maintenance, "RESERVED", "2026-11-13T10:00:00Z")));
    assertEquals(
        Outcome.INVALID_TRANSITION,
        service.assign(event("e2", maintenance, "AVAILABLE", "2026-11-13T10:00:00Z")));
    assertEquals(
        Outcome.INVALID_TRANSITION,
        service.assign(event("e3", available, "MAINTENANCE_CLEANING", "2026-11-13T10:00:00Z")));
    assertEquals(VesselStatus.MAINTENANCE, statusOf(maintenance));
    assertEquals(VesselStatus.AVAILABLE, statusOf(available));
  }

  @Test
  void theSameStateIsNotAChange() {
    Vessel b = boat(VesselStatus.RESERVED);

    assertEquals(
        Outcome.NO_CHANGE, service.assign(event("e1", b, "RESERVED", "2026-11-13T10:00:00Z")));
    assertTrue(vessels.history.isEmpty());
  }

  @Test
  void incompleteOrInvalidEventsAreRejectedWithTheFields() {
    Vessel b = boat(VesselStatus.AVAILABLE);

    ValidationFailedException error =
        assertThrows(
            ValidationFailedException.class,
            () -> service.assign(new StatusEvent(" ", "no-uuid", "FLYING", "ayer")));
    assertEquals(
        java.util.List.of("event_id", "vessel_id", "operational_status", "occurred_at"),
        error.violations().stream().map(v -> v.field()).toList());
    assertThrows(ValidationFailedException.class, () -> service.assign(null));
    assertThrows(
        ValidationFailedException.class,
        () -> service.assign(event("e1", b, null, "2026-11-13T10:00:00Z")));
  }

  @Test
  void anAvailableBoatCanGoStraightToNavigation() {
    Vessel available = boat(VesselStatus.AVAILABLE);

    assertEquals(
        Outcome.APPLIED,
        service.assign(event("e1", available, "IN_NAVIGATION", "2026-11-13T10:00:00Z")));
    assertEquals(VesselStatus.NAVIGATION, statusOf(available));
  }
}
