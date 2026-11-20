package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.FleetFilter;
import com.seashare.seasharem1.application.port.in.FleetItem;
import com.seashare.seasharem1.application.port.out.UserRepositoryPort;
import com.seashare.seasharem1.application.usecase.AdminFleetService;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.UserRole;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminFleetServiceTest {

  private static final User FERNANDO =
      new User(
          UUID.randomUUID(), "Fernando Fernández", 1010000002L, "f@t", "1", "x", UserRole.OWNER);
  private static final User MARIANA =
      new User(UUID.randomUUID(), "Mariana Gómez", 1010000003L, "m@t", "1", "x", UserRole.OWNER);

  private Fakes.Vessels vessels;
  private AdminFleetService service;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    UserRepositoryPort users =
        new UserRepositoryPort() {
          @Override
          public Optional<User> findByDocumentNumber(long documentNumber) {
            return Optional.empty();
          }

          @Override
          public Optional<User> findById(UUID id) {
            return Optional.empty();
          }

          @Override
          public List<User> findAllByRole(UserRole role) {
            return List.of();
          }

          @Override
          public List<User> findByIds(Collection<UUID> ids) {
            return List.of(FERNANDO, MARIANA).stream().filter(u -> ids.contains(u.id())).toList();
          }
        };
    service = new AdminFleetService(vessels, users, new Fakes.Ports());
  }

  private Vessel boat(User owner, String name, String registration, VesselStatus status, int day) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), owner.id(), name, registration, VesselType.YACHT, 8));
    Vessel done =
        draft.complete(
            "https://img.test/a.png",
            AdditionalServices.none(),
            new BigDecimal("1000"),
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, day, 10, 0));
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
            null,
            done.portId(),
            done.ownerId(),
            false,
            done.registeredAt()));
  }

  private void seed() {
    boat(FERNANDO, "Río Magdalena I", "CP-05-1042-A", VesselStatus.AVAILABLE, 1);
    boat(FERNANDO, "Atlántico Star", "CP-12-0891-B", VesselStatus.MAINTENANCE, 2);
    boat(MARIANA, "Caimán Express", "CP-08-2210-C", VesselStatus.AVAILABLE, 3);
    vessels.save(
        Vessel.newDraft(
            UUID.randomUUID(), MARIANA.id(), "Borrador", "CP-99-0000-Z", VesselType.YACHT, 8));
  }

  private List<String> names(FleetFilter filter) {
    return service.list(filter).stream().map(item -> item.vessel().name()).toList();
  }

  @Test
  void listsAllRegisteredBoatsNewestFirstWithoutDraftsAndWithTheirOwner() {
    seed();

    List<FleetItem> items = service.list(FleetFilter.none());

    assertEquals(
        List.of("Caimán Express", "Atlántico Star", "Río Magdalena I"),
        items.stream().map(i -> i.vessel().name()).toList());
    assertEquals("Mariana Gómez", items.get(0).owner().fullName());
  }

  @Test
  void deletedBoatsAreNotListed() {
    seed();
    Vessel gone =
        vessels.store.values().stream()
            .filter(v -> v.name().equals("Caimán Express"))
            .findFirst()
            .get();
    vessels.markDeleted(gone.id(), FERNANDO.id(), LocalDateTime.now());

    assertEquals(2, service.list(FleetFilter.none()).size());
  }

  @Test
  void filtersByOwner() {
    seed();
    assertEquals(
        List.of("Atlántico Star", "Río Magdalena I"),
        names(new FleetFilter(FERNANDO.id(), null, null)));
  }

  @Test
  void filtersByStatus() {
    seed();
    assertEquals(
        List.of("Atlántico Star"), names(new FleetFilter(null, VesselStatus.MAINTENANCE, null)));
  }

  @Test
  void searchesByOwnerNameWithoutAccentsOrCase() {
    seed();
    assertEquals(
        List.of("Atlántico Star", "Río Magdalena I"),
        names(new FleetFilter(null, null, "fernandez")));
    assertEquals(List.of("Caimán Express"), names(new FleetFilter(null, null, "  GOMEZ ")));
  }

  @Test
  void searchesByOwnerDocument() {
    seed();
    assertEquals(List.of("Caimán Express"), names(new FleetFilter(null, null, "1010000003")));
    assertEquals(3, names(new FleetFilter(null, null, "10100000")).size());
  }

  @Test
  void filtersCombine() {
    seed();
    assertEquals(
        List.of("Río Magdalena I"),
        names(new FleetFilter(FERNANDO.id(), VesselStatus.AVAILABLE, "fernando")));
    assertTrue(names(new FleetFilter(MARIANA.id(), VesselStatus.MAINTENANCE, null)).isEmpty());
  }

  @Test
  void ownersAreThoseWithRegisteredBoatsSortedByName() {
    seed();
    assertEquals(
        List.of("Fernando Fernández", "Mariana Gómez"),
        service.listOwners().stream().map(User::fullName).toList());
  }

  @Test
  void detailOfAnyRegisteredBoatRegardlessOfOwner() {
    seed();
    Vessel any = vessels.store.values().iterator().next();

    assertEquals(any.id(), service.getDetail(any.id()).vessel().id());
  }

  @Test
  void detailOfDraftOrUnknownBoatIs404() {
    seed();
    Vessel draft = vessels.store.values().stream().filter(Vessel::isDraft).findFirst().get();

    assertThrows(ResourceNotFoundException.class, () -> service.getDetail(draft.id()));
    assertThrows(ResourceNotFoundException.class, () -> service.getDetail(UUID.randomUUID()));
  }
}
