package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.usecase.DeleteBoatService;
import com.seashare.seasharem1.domain.exception.BoatNotDeletableException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
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

class DeleteBoatServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private Fakes.Vessels vessels;
  private DeleteBoatService service;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    Clock clock = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneOffset.UTC);
    service = new DeleteBoatService(vessels, clock);
  }

  private Vessel registered(String registration) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                OWNER,
                "Río Magdalena I",
                registration,
                VesselType.MOTORBOAT,
                8));
    return vessels.save(
        draft.complete(
            "https://img.test/a.png",
            new AdditionalServices(true, false, false, false, false),
            new BigDecimal("180000"),
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, 4, 10, 0)));
  }

  private Vessel withStatus(Vessel vessel, VesselStatus status) {
    return vessels.save(
        new Vessel(
            vessel.id(),
            vessel.name(),
            vessel.legalRegistration(),
            vessel.type(),
            vessel.maxCapacity(),
            vessel.photoPath(),
            vessel.hasLifeJackets(),
            vessel.hasCoolerWithIce(),
            vessel.hasSoundSystem(),
            vessel.hasFishingGear(),
            vessel.hasDivingGear(),
            vessel.baseRate(),
            status,
            null,
            vessel.portId(),
            vessel.ownerId(),
            false,
            vessel.registeredAt()));
  }

  @Test
  void deletesAnAvailableBoatAndRecordsWhoDidIt() {
    Vessel vessel = registered("CP-01-0001-A");

    service.deleteBoat(OWNER, vessel.id());

    assertTrue(vessels.store.get(vessel.id()).deleted());
    assertEquals(OWNER, vessels.deletedLog.get(vessel.id()));
    assertTrue(vessels.findById(vessel.id()).isEmpty());
  }

  @Test
  void deletesABoatInMaintenance() {
    Vessel vessel = withStatus(registered("CP-01-0001-A"), VesselStatus.MAINTENANCE);

    service.deleteBoat(OWNER, vessel.id());

    assertTrue(vessels.store.get(vessel.id()).deleted());
  }

  @Test
  void aReservedOrNavigatingBoatIsNotDeletedAndReportsItsStatus() {
    for (VesselStatus status :
        new VesselStatus[] {VesselStatus.RESERVED, VesselStatus.NAVIGATION}) {
      Vessel vessel = withStatus(registered("CP-0" + status.ordinal() + "-0001-A"), status);

      BoatNotDeletableException failure =
          assertThrows(
              BoatNotDeletableException.class, () -> service.deleteBoat(OWNER, vessel.id()));

      assertEquals(status, failure.currentStatus());
      assertFalse(vessels.store.get(vessel.id()).deleted());
    }
  }

  @Test
  void anotherOwnersBoatLooksLikeItDoesNotExist() {
    Vessel vessel = registered("CP-01-0001-A");

    assertThrows(ResourceNotFoundException.class, () -> service.deleteBoat(OTHER, vessel.id()));
    assertFalse(vessels.store.get(vessel.id()).deleted());
  }

  @Test
  void unknownIdAndDraftAreNotFound() {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), OWNER, "Borrador", "CP-09-0009-A", VesselType.YACHT, 10));

    assertThrows(
        ResourceNotFoundException.class, () -> service.deleteBoat(OWNER, UUID.randomUUID()));
    assertThrows(ResourceNotFoundException.class, () -> service.deleteBoat(OWNER, draft.id()));
  }

  @Test
  void deletingTwiceTheSecondTimeIsNotFoundAndKeepsTheFirstRecord() {
    Vessel vessel = registered("CP-01-0001-A");
    service.deleteBoat(OWNER, vessel.id());

    assertThrows(ResourceNotFoundException.class, () -> service.deleteBoat(OWNER, vessel.id()));
    assertEquals(1, vessels.deletedLog.size());
  }

  @Test
  void theRegistrationIsFreedAfterDeleting() {
    Vessel vessel = registered("CP-01-0001-A");
    assertTrue(vessels.existsByLegalRegistration("CP-01-0001-A"));

    service.deleteBoat(OWNER, vessel.id());

    assertFalse(vessels.existsByLegalRegistration("CP-01-0001-A"));
    registered("CP-01-0001-A");
  }
}
