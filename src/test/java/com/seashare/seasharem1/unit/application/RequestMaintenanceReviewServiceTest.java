package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.usecase.RequestMaintenanceReviewService;
import com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RequestMaintenanceReviewServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private final User admin1 = user("Admin Uno", 1010000001L, UserRole.ADMIN);
  private final User admin2 = user("Admin Dos", 1010000009L, UserRole.ADMIN);
  private final User owner =
      new User(OWNER, "Dueño", 1010000002L, "d@t", "3100000000", "x", UserRole.OWNER);

  private Fakes.Vessels vessels;
  private Fakes.Notifications notifications;
  private RequestMaintenanceReviewService service;
  private int sequence;

  private static User user(String name, long doc, UserRole role) {
    return new User(UUID.randomUUID(), name, doc, name + "@t", "3100000000", "x", role);
  }

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    notifications = new Fakes.Notifications();
    List<User> all = List.of(admin1, owner, admin2);
    UserRepositoryPort users =
        new UserRepositoryPort() {
          @Override
          public Optional<User> findByDocumentNumber(long documentNumber) {
            return all.stream().filter(u -> u.documentNumber() == documentNumber).findFirst();
          }

          @Override
          public Optional<User> findById(UUID id) {
            return all.stream().filter(u -> u.id().equals(id)).findFirst();
          }

          @Override
          public List<User> findAllByRole(UserRole role) {
            return all.stream().filter(u -> u.role() == role).toList();
          }

          @Override
          public List<User> findByIds(Collection<UUID> ids) {
            return all.stream().filter(u -> ids.contains(u.id())).toList();
          }
        };
    Clock clock = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneOffset.UTC);
    service = new RequestMaintenanceReviewService(vessels, users, notifications, clock);
  }

  private Vessel boat(VesselStatus status, MaintenanceBy by, UUID ownerId) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                ownerId,
                "Atlántico Star",
                "CP-12-" + (800 + sequence++) + "-B",
                VesselType.YACHT,
                10));
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
  void notifiesEveryAdminAndKeepsTheBoatInMaintenance() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, OWNER);

    service.requestReview(OWNER, vessel.id());

    assertEquals(2, notifications.store.size());
    for (Notification notice : notifications.store) {
      assertEquals(NotificationType.REVIEW_REQUEST, notice.type());
      assertEquals(vessel.id(), notice.vesselId());
      assertFalse(notice.read());
      assertTrue(
          notice
              .message()
              .startsWith(
                  "El propietario solicitó la revisión de la embarcación Atlántico Star (CP-12-"));
      assertTrue(notice.message().endsWith("el 12 de noviembre de 2026."));
    }
    assertEquals(
        List.of(admin1.id(), admin2.id()),
        notifications.store.stream().map(Notification::recipientId).toList());
    assertEquals(VesselStatus.MAINTENANCE, vessels.store.get(vessel.id()).status());
  }

  @Test
  void repeatingWhileTheAdminHasNotReadItDoesNotDuplicate() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, OWNER);

    service.requestReview(OWNER, vessel.id());
    service.requestReview(OWNER, vessel.id());

    assertEquals(2, notifications.store.size());
  }

  @Test
  void aSecondRequestIsIgnoredEvenAfterTheAdminReadTheNotice() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, OWNER);
    service.requestReview(OWNER, vessel.id());
    notifications.markAllRead(admin1.id());
    notifications.markAllRead(admin2.id());

    service.requestReview(OWNER, vessel.id());

    assertEquals(2, notifications.store.size());
    assertTrue(vessels.store.get(vessel.id()).reviewRequested());
  }

  @Test
  void aNewAdminMaintenanceAllowsRequestingTheReviewAgain() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, OWNER);
    service.requestReview(OWNER, vessel.id());

    vessels.finishMaintenanceAsAdmin(vessel.id());
    vessels.startMaintenanceAsAdmin(
        vessel.id(),
        new com.seashare.seasharem1.domain.model.MaintenanceWindow(
            LocalDateTime.of(2026, 11, 4, 0, 0), LocalDateTime.of(2026, 11, 10, 0, 0)));
    assertFalse(vessels.store.get(vessel.id()).reviewRequested());
    service.requestReview(OWNER, vessel.id());

    assertEquals(4, notifications.store.size());
  }

  @Test
  void onlyAMaintenanceSentByTheAdminCanBeReviewed() {
    for (Vessel vessel :
        new Vessel[] {
          boat(VesselStatus.MAINTENANCE, MaintenanceBy.OWNER, OWNER),
          boat(VesselStatus.MAINTENANCE, null, OWNER),
          boat(VesselStatus.AVAILABLE, null, OWNER),
          boat(VesselStatus.RESERVED, null, OWNER)
        }) {
      BoatMaintenanceConflictException failure =
          assertThrows(
              BoatMaintenanceConflictException.class,
              () -> service.requestReview(OWNER, vessel.id()));
      assertEquals(vessel.status(), failure.currentStatus());
    }
    assertTrue(notifications.store.isEmpty());
  }

  @Test
  void anotherOwnersUnknownOrDeletedBoatIsNotFound() {
    Vessel theirs = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, UUID.randomUUID());
    Vessel deleted = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, OWNER);
    vessels.markDeleted(deleted.id(), admin1.id(), LocalDateTime.now());

    assertThrows(ResourceNotFoundException.class, () -> service.requestReview(OWNER, theirs.id()));
    assertThrows(
        ResourceNotFoundException.class, () -> service.requestReview(OWNER, UUID.randomUUID()));
    assertThrows(ResourceNotFoundException.class, () -> service.requestReview(OWNER, deleted.id()));
  }

  @Test
  void aFailingNoticeDoesNotBreakTheRequest() {
    Vessel vessel = boat(VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN, OWNER);
    notifications.failing = true;

    service.requestReview(OWNER, vessel.id());

    assertTrue(notifications.store.isEmpty());
  }
}
