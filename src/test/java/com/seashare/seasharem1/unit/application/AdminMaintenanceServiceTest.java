package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.usecase.AdminMaintenanceService;
import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.MaintenanceBy;
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

class AdminMaintenanceServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID ADMIN = UUID.randomUUID();
  private static final String REASON = "Revisión obligatoria del motor.";
  private static final Instant UNTIL = Instant.parse("2026-11-10T00:00:00Z");

  private int sequence;
  private Fakes.Vessels vessels;
  private Fakes.Notifications notifications;
  private AdminMaintenanceService service;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    notifications = new Fakes.Notifications();
    // 4 de noviembre de 2026, 02:00 UTC = 3 de noviembre, 21:00 en Colombia
    Clock clock = Clock.fixed(Instant.parse("2026-11-04T02:00:00Z"), ZoneOffset.UTC);
    service = new AdminMaintenanceService(vessels, new Fakes.Ports(), notifications, clock);
  }

  private Vessel boat(VesselStatus status, MaintenanceBy by) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                OWNER,
                "Río Magdalena I",
                sequence++ == 0 ? "CP-05-1042-A" : "CP-05-" + (2000 + sequence) + "-A",
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
  void sendsAnyOwnersBoatToMaintenanceRecordsAdminAndNotifiesWithTheReason() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    var result = service.sendToMaintenance(ADMIN, vessel.id(), "  " + REASON + "  ", null, UNTIL);

    assertEquals(VesselStatus.MAINTENANCE, result.vessel().status());
    assertEquals(MaintenanceBy.ADMIN, result.vessel().maintenanceBy());
    assertEquals(1, notifications.store.size());
    Notification notice = notifications.store.get(0);
    assertEquals(OWNER, notice.recipientId());
    assertEquals(NotificationType.MAINTENANCE_NOTICE, notice.type());
    assertEquals(
        "Tu embarcación Río Magdalena I (CP-05-1042-A) fue enviada a mantenimiento por un administrador el 3 de noviembre de 2026. Motivo: "
            + REASON,
        notice.message());
  }

  @Test
  void aMissingBlankOrTooLongReasonIs400AndChangesNothing() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    for (String reason : new String[] {null, "", "  ", "x".repeat(301)}) {
      ValidationFailedException failure =
          assertThrows(
              ValidationFailedException.class,
              () -> service.sendToMaintenance(ADMIN, vessel.id(), reason, null, UNTIL));
      assertEquals("reason", failure.violations().get(0).field());
    }
    assertEquals(VesselStatus.AVAILABLE, vessels.store.get(vessel.id()).status());
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void reservedOrNavigatingBoatsAreRefusedWithTheirStatusAndNoNotice() {
    for (VesselStatus status :
        new VesselStatus[] {VesselStatus.RESERVED, VesselStatus.NAVIGATION}) {
      Vessel vessel = boat(status, null);
      BoatMaintenanceConflictException failure =
          assertThrows(
              BoatMaintenanceConflictException.class,
              () -> service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, UNTIL));
      assertEquals(status, failure.currentStatus());
    }
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void sendingTwiceIsIdempotentAndDoesNotNotifyAgain() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);
    service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, UNTIL);

    var again = service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, UNTIL);

    assertEquals(VesselStatus.MAINTENANCE, again.vessel().status());
    assertEquals(1, notifications.store.size());
  }

  @Test
  void aBoatAlreadySentByTheOwnerKeepsItsOriginWhenTheAdminSendsIt() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.OWNER);

    var result = service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, UNTIL);

    assertEquals(MaintenanceBy.OWNER, result.vessel().maintenanceBy());
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void finishesTheMaintenanceTheAdminStartedAndNotifiesTheOwner() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN);

    var result = service.finishMaintenance(ADMIN, vessel.id());

    assertEquals(VesselStatus.AVAILABLE, result.vessel().status());
    assertNull(result.vessel().maintenanceBy());
    assertEquals(1, notifications.store.size());
    assertEquals(NotificationType.VESSEL_AVAILABLE, notifications.store.get(0).type());
    assertEquals(
        "Tu embarcación Río Magdalena I (CP-05-1042-A) fue habilitada por un administrador el 3 de noviembre de 2026. Ya puede recibir reservas.",
        notifications.store.get(0).message());
  }

  @Test
  void theAdminCannotFinishAMaintenanceStartedByTheOwnerOrTheSystem() {
    for (MaintenanceBy by : new MaintenanceBy[] {MaintenanceBy.OWNER, null}) {
      Vessel vessel = boat(VesselStatus.MAINTENANCE, by);

      BoatMaintenanceConflictException failure =
          assertThrows(
              BoatMaintenanceConflictException.class,
              () -> service.finishMaintenance(ADMIN, vessel.id()));

      assertEquals(VesselStatus.MAINTENANCE, failure.currentStatus());
      assertEquals(by, failure.maintenanceBy());
      assertEquals(VesselStatus.MAINTENANCE, vessels.store.get(vessel.id()).status());
    }
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void finishingAnAvailableBoatIsIdempotentWithoutNotice() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    assertEquals(
        VesselStatus.AVAILABLE, service.finishMaintenance(ADMIN, vessel.id()).vessel().status());
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void finishingAReservedBoatIsAConflict() {
    Vessel vessel = boat(VesselStatus.RESERVED, null);

    assertThrows(
        BoatMaintenanceConflictException.class,
        () -> service.finishMaintenance(ADMIN, vessel.id()));
  }

  @Test
  void unknownDraftAndDeletedBoatsAreNotFound() {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), OWNER, "Borrador", "CP-09-9-A", VesselType.YACHT, 10));
    Vessel deleted = boat(VesselStatus.AVAILABLE, null);
    vessels.markDeleted(deleted.id(), ADMIN, LocalDateTime.now());

    assertThrows(
        ResourceNotFoundException.class,
        () -> service.sendToMaintenance(ADMIN, UUID.randomUUID(), REASON, null, UNTIL));
    assertThrows(
        ResourceNotFoundException.class,
        () -> service.sendToMaintenance(ADMIN, draft.id(), REASON, null, UNTIL));
    assertThrows(
        ResourceNotFoundException.class, () -> service.finishMaintenance(ADMIN, deleted.id()));
  }

  @Test
  void aFailingNoticeDoesNotUndoTheChange() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);
    notifications.failing = true;

    service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, UNTIL);

    assertEquals(VesselStatus.MAINTENANCE, vessels.store.get(vessel.id()).status());
    assertFalse(vessels.store.get(vessel.id()).deleted());
  }

  @Test
  void sendingToMaintenanceRequiresAFutureEnd() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    assertThrows(
        ValidationFailedException.class,
        () -> service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, null));
    assertEquals(VesselStatus.AVAILABLE, vessels.store.get(vessel.id()).status());
  }

  @Test
  void sendingToMaintenanceStoresTheWindow() {
    Vessel vessel = boat(VesselStatus.AVAILABLE, null);

    service.sendToMaintenance(ADMIN, vessel.id(), REASON, null, UNTIL);

    assertEquals(LocalDateTime.of(2026, 11, 10, 0, 0), vessels.windows.get(vessel.id()).until());
  }
}
