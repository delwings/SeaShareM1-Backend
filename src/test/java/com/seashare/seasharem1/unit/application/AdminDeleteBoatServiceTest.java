package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.usecase.AdminDeleteBoatService;
import com.seashare.seasharem1.domain.exception.BoatNotDeletableException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
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

class AdminDeleteBoatServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID ADMIN = UUID.randomUUID();
  private static final String REASON = "Fotos que no corresponden a la embarcación.";

  private Fakes.Vessels vessels;
  private Fakes.Notifications notifications;
  private AdminDeleteBoatService service;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    notifications = new Fakes.Notifications();
    // 4 de noviembre de 2026, 02:00 UTC = 3 de noviembre, 21:00 en Colombia
    Clock clock = Clock.fixed(Instant.parse("2026-11-04T02:00:00Z"), ZoneOffset.UTC);
    service = new AdminDeleteBoatService(vessels, notifications, clock);
  }

  private Vessel registered(String name, String registration) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(UUID.randomUUID(), OWNER, name, registration, VesselType.MOTORBOAT, 8));
    return vessels.save(
        draft.complete(
            "https://img.test/a.png",
            new AdditionalServices(true, false, false, false, false),
            new BigDecimal("180000"),
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, 1, 10, 0)));
  }

  private Vessel withStatus(Vessel v, VesselStatus status) {
    return vessels.save(
        new Vessel(
            v.id(),
            v.name(),
            v.legalRegistration(),
            v.type(),
            v.maxCapacity(),
            v.photoPath(),
            v.hasLifeJackets(),
            v.hasCoolerWithIce(),
            v.hasSoundSystem(),
            v.hasFishingGear(),
            v.hasDivingGear(),
            v.baseRate(),
            status,
            null,
            v.portId(),
            v.ownerId(),
            false,
            v.registeredAt()));
  }

  @Test
  void deletesAnotherOwnersBoatAndRecordsTheAdmin() {
    Vessel vessel = registered("Río Magdalena I", "CP-01-0001-A");

    service.deleteBoat(ADMIN, vessel.id(), REASON);

    assertTrue(vessels.store.get(vessel.id()).deleted());
    assertEquals(ADMIN, vessels.deletedLog.get(vessel.id()));
  }

  @Test
  void notifiesTheOwnerOnceWithNameRegistrationAndColombianDate() {
    Vessel vessel = registered("Río Magdalena I", "CP-01-0001-A");

    service.deleteBoat(ADMIN, vessel.id(), REASON);

    assertEquals(1, notifications.store.size());
    Notification notification = notifications.store.get(0);
    assertEquals(OWNER, notification.recipientId());
    assertEquals(vessel.id(), notification.vesselId());
    assertEquals(NotificationType.VESSEL_DELETED, notification.type());
    assertFalse(notification.read());
    assertEquals(
        "Tu embarcación Río Magdalena I (CP-01-0001-A) fue eliminada por un administrador el 3 de noviembre de 2026. Motivo: Fotos que no corresponden a la embarcación.",
        notification.message());
  }

  @Test
  void deletesABoatInMaintenance() {
    Vessel vessel = withStatus(registered("Uno", "CP-01-0001-A"), VesselStatus.MAINTENANCE);

    service.deleteBoat(ADMIN, vessel.id(), REASON);

    assertTrue(vessels.store.get(vessel.id()).deleted());
    assertEquals(1, notifications.store.size());
  }

  @Test
  void aReservedOrNavigatingBoatIsNotDeletedNorNotified() {
    for (VesselStatus status :
        new VesselStatus[] {VesselStatus.RESERVED, VesselStatus.NAVIGATION}) {
      Vessel vessel =
          withStatus(registered("B" + status, "CP-0" + status.ordinal() + "-1-A"), status);

      BoatNotDeletableException failure =
          assertThrows(
              BoatNotDeletableException.class,
              () -> service.deleteBoat(ADMIN, vessel.id(), REASON));

      assertEquals(status, failure.currentStatus());
      assertFalse(vessels.store.get(vessel.id()).deleted());
    }
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void unknownDraftAndAlreadyDeletedAreNotFound() {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), OWNER, "Borrador", "CP-09-9-A", VesselType.YACHT, 10));
    Vessel done = registered("Dos", "CP-02-0002-A");
    service.deleteBoat(ADMIN, done.id(), REASON);
    notifications.store.clear();

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.deleteBoat(ADMIN, UUID.randomUUID(), REASON));
    assertThrows(
        ResourceNotFoundException.class, () -> service.deleteBoat(ADMIN, draft.id(), REASON));
    assertThrows(
        ResourceNotFoundException.class, () -> service.deleteBoat(ADMIN, done.id(), REASON));
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void aFailingNotificationDoesNotUndoTheDeletion() {
    Vessel vessel = registered("Uno", "CP-01-0001-A");
    notifications.failing = true;

    service.deleteBoat(ADMIN, vessel.id(), REASON);

    assertTrue(vessels.store.get(vessel.id()).deleted());
  }

  @Test
  void aMissingBlankOrTooLongReasonIsRejectedBeforeAnythingChanges() {
    Vessel vessel = registered("Uno", "CP-01-0001-A");

    for (String reason : new String[] {null, "", "   ", "x".repeat(301)}) {
      ValidationFailedException failure =
          assertThrows(
              ValidationFailedException.class,
              () -> service.deleteBoat(ADMIN, vessel.id(), reason));
      assertEquals("reason", failure.violations().get(0).field());
    }
    assertFalse(vessels.store.get(vessel.id()).deleted());
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void theReasonIsTrimmedAndAMaximumLengthReasonIsAccepted() {
    Vessel vessel = registered("Uno", "CP-01-0001-A");

    service.deleteBoat(ADMIN, vessel.id(), "  " + "x".repeat(300) + "  ");

    assertTrue(notifications.store.get(0).message().endsWith("Motivo: " + "x".repeat(300)));
  }
}
