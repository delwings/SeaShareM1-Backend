package com.seashare.seasharem1.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainModelsTest {

  private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static Vessel vessel(VesselStatus status) {
    return new Vessel(
        ID,
        "Atlántico Star",
        "CP-05-1042-A",
        VesselType.YACHT,
        15,
        "https://example.test/photo.jpg",
        true,
        false,
        true,
        false,
        true,
        new BigDecimal("180000.00"),
        status,
        MaintenanceBy.ADMIN,
        OTHER_ID,
        ID,
        false,
        null);
  }

  @Test
  void userExposesItsData() {
    User user = new User(ID, "Laura Gómez", 1010000001L, "a@b.test", "300", "hash", UserRole.ADMIN);

    assertEquals(ID, user.id());
    assertEquals("Laura Gómez", user.fullName());
    assertEquals(1010000001L, user.documentNumber());
    assertEquals("a@b.test", user.email());
    assertEquals("300", user.phone());
    assertEquals("hash", user.password());
    assertEquals(UserRole.ADMIN, user.role());
  }

  @Test
  void userKnowsIfItIsAdmin() {
    assertTrue(new User(ID, "A", 1L, "e", "p", "h", UserRole.ADMIN).isAdmin());
    assertFalse(new User(ID, "A", 1L, "e", "p", "h", UserRole.OWNER).isAdmin());
  }

  @Test
  void portExposesItsData() {
    Port port =
        new Port(ID, "Cartagena", new BigDecimal("10.391049"), new BigDecimal("-75.479426"));

    assertEquals(ID, port.id());
    assertEquals("Cartagena", port.name());
    assertEquals(new BigDecimal("10.391049"), port.latitude());
    assertEquals(new BigDecimal("-75.479426"), port.longitude());
  }

  @Test
  void vesselExposesItsData() {
    Vessel vessel = vessel(VesselStatus.AVAILABLE);

    assertEquals(ID, vessel.id());
    assertEquals("Atlántico Star", vessel.name());
    assertEquals("CP-05-1042-A", vessel.legalRegistration());
    assertEquals(VesselType.YACHT, vessel.type());
    assertEquals(15, vessel.maxCapacity());
    assertEquals("https://example.test/photo.jpg", vessel.photoPath());
    assertEquals(true, vessel.hasLifeJackets());
    assertEquals(false, vessel.hasCoolerWithIce());
    assertEquals(true, vessel.hasSoundSystem());
    assertEquals(false, vessel.hasFishingGear());
    assertEquals(true, vessel.hasDivingGear());
    assertEquals(new BigDecimal("180000.00"), vessel.baseRate());
    assertEquals(VesselStatus.AVAILABLE, vessel.status());
    assertEquals(MaintenanceBy.ADMIN, vessel.maintenanceBy());
    assertEquals(OTHER_ID, vessel.portId());
    assertEquals(ID, vessel.ownerId());
    assertFalse(vessel.deleted());
  }

  @Test
  void vesselKnowsIfItIsADraft() {
    assertTrue(vessel(VesselStatus.DRAFT).isDraft());
    assertFalse(vessel(VesselStatus.AVAILABLE).isDraft());
  }

  @Test
  void notificationExposesItsData() {
    LocalDateTime now = LocalDateTime.of(2026, 11, 2, 20, 0);
    Notification notification =
        new Notification(ID, OTHER_ID, ID, NotificationType.REVIEW_REQUEST, "Revisar", false, now);

    assertEquals(ID, notification.id());
    assertEquals(OTHER_ID, notification.recipientId());
    assertEquals(ID, notification.vesselId());
    assertEquals(NotificationType.REVIEW_REQUEST, notification.type());
    assertEquals("Revisar", notification.message());
    assertFalse(notification.read());
    assertEquals(now, notification.createdAt());
  }
}
