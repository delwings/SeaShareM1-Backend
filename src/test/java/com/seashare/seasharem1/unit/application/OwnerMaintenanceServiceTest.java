package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.seashare.seasharem1.application.usecase.OwnerMaintenanceService;
import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
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

class OwnerMaintenanceServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private static final Instant UNTIL = Instant.parse("2026-11-10T00:00:00Z");

  private Fakes.Vessels vessels;
  private OwnerMaintenanceService service;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    Clock clock = Clock.fixed(Instant.parse("2026-11-04T02:00:00Z"), ZoneOffset.UTC);
    service = new OwnerMaintenanceService(vessels, new Fakes.Ports(), clock);
  }

  private Vessel boat(VesselStatus status, MaintenanceBy by) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                OWNER,
                "Río Magdalena I",
                "CP-05-1042-A",
                VesselType.MOTORBOAT,
                8));
    Vessel done =
        draft.complete(
            "https://img.test/a.png",
            AdditionalServices.none(),
            new BigDecimal("180000"),
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, 4, 10, 0));
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
            by,
            done.portId(),
            done.ownerId(),
            false,
            done.registeredAt()));
  }

  @Test
  void sendsAnAvailableBoatToMaintenanceAndRecordsTheOwner() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    var result = service.sendToMaintenance(OWNER, vessel.id(), null, UNTIL);

    assertEquals(VesselStatus.MAINTENANCE, result.vessel().status());
    assertEquals(MaintenanceBy.OWNER, vessels.store.get(vessel.id()).maintenanceBy());
  }

  @Test
  void sendingABoatAlreadyInMaintenanceKeepsItsOrigin() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN);

    var result = service.sendToMaintenance(OWNER, vessel.id(), null, UNTIL);

    assertEquals(VesselStatus.MAINTENANCE, result.vessel().status());
    assertEquals(MaintenanceBy.ADMIN, vessels.store.get(vessel.id()).maintenanceBy());
  }

  @Test
  void aReservedOrNavigatingBoatCannotBeSent() {
    for (VesselStatus status :
        new VesselStatus[] {VesselStatus.RESERVED, VesselStatus.NAVIGATION}) {
      Vessel vessel = boat(status, null);

      BoatMaintenanceConflictException failure =
          assertThrows(
              BoatMaintenanceConflictException.class,
              () -> service.sendToMaintenance(OWNER, vessel.id(), null, UNTIL));

      assertEquals(status, failure.currentStatus());
      assertEquals(status, vessels.store.get(vessel.id()).status());
      vessels.store.clear();
    }
  }

  @Test
  void finishesMaintenanceStartedByTheOwner() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.OWNER);

    var result = service.finishMaintenance(OWNER, vessel.id());

    assertEquals(VesselStatus.AVAILABLE, result.vessel().status());
    assertNull(vessels.store.get(vessel.id()).maintenanceBy());
  }

  @Test
  void finishesMaintenanceWithoutAnOrigin() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, null);

    assertEquals(
        VesselStatus.AVAILABLE, service.finishMaintenance(OWNER, vessel.id()).vessel().status());
  }

  @Test
  void finishingABoatAlreadyAvailableChangesNothing() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    assertEquals(
        VesselStatus.AVAILABLE, service.finishMaintenance(OWNER, vessel.id()).vessel().status());
  }

  @Test
  void theOwnerCannotLiftAnAdminSuspension() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN);

    BoatMaintenanceConflictException failure =
        assertThrows(
            BoatMaintenanceConflictException.class,
            () -> service.finishMaintenance(OWNER, vessel.id()));

    assertEquals(VesselStatus.MAINTENANCE, failure.currentStatus());
    assertEquals(MaintenanceBy.ADMIN, failure.maintenanceBy());
    assertEquals(VesselStatus.MAINTENANCE, vessels.store.get(vessel.id()).status());
  }

  @Test
  void aReservedBoatCannotFinishMaintenance() {
    Vessel vessel = boat(VesselStatus.RESERVED, null);

    BoatMaintenanceConflictException failure =
        assertThrows(
            BoatMaintenanceConflictException.class,
            () -> service.finishMaintenance(OWNER, vessel.id()));

    assertEquals(VesselStatus.RESERVED, failure.currentStatus());
  }

  @Test
  void anotherOwnersOrUnknownBoatLooksLikeItDoesNotExist() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.sendToMaintenance(OTHER, vessel.id(), null, UNTIL));
    assertThrows(
        ResourceNotFoundException.class, () -> service.finishMaintenance(OTHER, vessel.id()));
    assertThrows(
        ResourceNotFoundException.class,
        () -> service.sendToMaintenance(OWNER, UUID.randomUUID(), null, UNTIL));
  }

  @Test
  void aDraftLooksLikeItDoesNotExist() {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), OWNER, "Borrador", "CP-09-0001-A", VesselType.YACHT, 8));

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.sendToMaintenance(OWNER, draft.id(), null, UNTIL));
  }

  @Test
  void sendingToMaintenanceRequiresAFutureEndAfterTheStart() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    assertThrows(
        ValidationFailedException.class,
        () -> service.sendToMaintenance(OWNER, vessel.id(), null, null));
    assertThrows(
        ValidationFailedException.class,
        () ->
            service.sendToMaintenance(
                OWNER, vessel.id(), null, Instant.parse("2026-11-03T00:00:00Z")));
    assertThrows(
        ValidationFailedException.class,
        () ->
            service.sendToMaintenance(
                OWNER, vessel.id(), Instant.parse("2026-11-11T00:00:00Z"), UNTIL));
    assertEquals(VesselStatus.AVAILABLE, vessels.store.get(vessel.id()).status());
  }

  @Test
  void sendingToMaintenanceStoresTheWindowStartingNowByDefault() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    service.sendToMaintenance(OWNER, vessel.id(), null, UNTIL);

    var window = vessels.windows.get(vessel.id());
    assertEquals(LocalDateTime.of(2026, 11, 4, 2, 0), window.start());
    assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0), window.until());
  }
}
