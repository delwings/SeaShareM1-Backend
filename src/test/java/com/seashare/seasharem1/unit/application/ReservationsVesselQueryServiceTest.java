package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.OperationalStatus;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.VesselInfo;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.usecase.ReservationsVesselQueryService;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Port;
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

class ReservationsVesselQueryServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-13T17:05:00Z");
  private final User owner =
      new User(
          UUID.randomUUID(),
          "Fernando Fernández",
          1010000002L,
          "f@t",
          "3100000000",
          "x",
          UserRole.OWNER);

  private Fakes.Vessels vessels;
  private Fakes.Ports ports;
  private ReservationsVesselQueryService service;
  private Port port;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    ports = new Fakes.Ports();
    port =
        ports.save(
            new Port(
                UUID.randomUUID(),
                "Marina Magdalena",
                new BigDecimal("11.2408"),
                new BigDecimal("-74.1990")));
    UserRepositoryPort users =
        new UserRepositoryPort() {
          @Override
          public Optional<User> findByDocumentNumber(long documentNumber) {
            return Optional.empty();
          }

          @Override
          public Optional<User> findById(UUID id) {
            return owner.id().equals(id) ? Optional.of(owner) : Optional.empty();
          }

          @Override
          public List<User> findAllByRole(UserRole role) {
            return List.of();
          }

          @Override
          public List<User> findByIds(Collection<UUID> ids) {
            return List.of();
          }
        };
    service =
        new ReservationsVesselQueryService(vessels, ports, users, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private Vessel registered(AdditionalServices services) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                owner.id(),
                "Río Magdalena II",
                "CP-05-1042-A",
                VesselType.MOTORBOAT,
                8));
    return vessels.save(
        draft.complete(
            "https://img.test/a.png",
            services,
            new BigDecimal("180000"),
            port.id(),
            LocalDateTime.of(2026, 11, 4, 10, 0)));
  }

  @Test
  void infoReturnsBothRegistrationStepsAndTheStatusButNoRate() {
    Vessel vessel = registered(new AdditionalServices(true, false, true, false, false));

    VesselInfo info = service.getInfo(vessel.id());

    assertEquals("Río Magdalena II", info.name());
    assertEquals("CP-05-1042-A", info.legalRegistration());
    assertEquals(VesselType.MOTORBOAT, info.type());
    assertEquals(8, info.maxCapacity());
    assertEquals(VesselStatus.AVAILABLE, info.status());
    assertEquals("https://img.test/a.png", info.photoUrl());
    assertEquals("Marina Magdalena", info.port().name());
    assertEquals(owner.id(), info.ownerId());
    assertEquals("Fernando Fernández", info.ownerName());
    assertEquals(
        List.of("Capitán", "Combustible", "Chalecos salvavidas", "Equipo de sonido"),
        info.amenities());
  }

  @Test
  void infoWithoutAdditionalServicesListsOnlyTheBaseOnes() {
    Vessel vessel = registered(AdditionalServices.none());

    assertEquals(List.of("Capitán", "Combustible"), service.getInfo(vessel.id()).amenities());
  }

  @Test
  void statusReturnsOnlyTheCurrentStateAndTheQueryMoment() {
    Vessel vessel = registered(AdditionalServices.none());

    OperationalStatus status = service.getOperationalStatus(vessel.id());

    assertEquals(vessel.id(), status.vesselId());
    assertEquals(VesselStatus.AVAILABLE, status.status());
    assertEquals(NOW, status.queriedAt());
  }

  @Test
  void statusReflectsEachOperationalStateInRealTime() {
    Vessel vessel = registered(AdditionalServices.none());
    for (VesselStatus state :
        List.of(VesselStatus.RESERVED, VesselStatus.NAVIGATION, VesselStatus.MAINTENANCE)) {
      vessels.store.put(
          vessel.id(),
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
              state,
              vessel.maintenanceBy(),
              vessel.portId(),
              vessel.ownerId(),
              vessel.deleted(),
              vessel.registeredAt()));

      assertEquals(state, service.getOperationalStatus(vessel.id()).status());
    }
  }

  @Test
  void unknownDeletedAndDraftBoatsAreNotFoundInBothQueries() {
    Vessel deleted = registered(AdditionalServices.none());
    vessels.store.put(deleted.id(), deleted.asDeleted());
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), owner.id(), "Borrador", "CP-05-2000-A", VesselType.YACHT, 6));

    for (UUID id : List.of(UUID.randomUUID(), deleted.id(), draft.id())) {
      assertThrows(ResourceNotFoundException.class, () -> service.getInfo(id));
      assertThrows(ResourceNotFoundException.class, () -> service.getOperationalStatus(id));
    }
    assertFalse(vessels.store.get(draft.id()).deleted());
  }
}
