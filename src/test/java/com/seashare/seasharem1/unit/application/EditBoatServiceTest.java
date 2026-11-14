package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.in.EditBoatCommand;
import com.seashare.seasharem1.application.usecase.EditBoatService;
import com.seashare.seasharem1.domain.exception.BoatNotEditableException;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditBoatServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();
  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

  private Fakes.Vessels vessels;
  private Fakes.Ports ports;
  private Fakes.Images images;
  private EditBoatService service;
  private Port originalPort;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    ports = vessels.ports;
    images = new Fakes.Images();
    service = new EditBoatService(vessels, ports, images);
    originalPort =
        ports.save(
            new Port(
                UUID.randomUUID(),
                "Marina Central",
                new BigDecimal("10.4000"),
                new BigDecimal("-75.5000")));
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
            "https://img.test/old.png",
            new AdditionalServices(true, false, false, false, false),
            new BigDecimal("180000"),
            originalPort.id(),
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

  private static EditBoatCommand command(UUID owner, UUID boatId) {
    return new EditBoatCommand(
        owner,
        boatId,
        "Río Magdalena II",
        "9",
        "Marina Central",
        "10.4",
        "-75.5",
        "200000",
        null,
        null);
  }

  private static EditBoatCommand withName(EditBoatCommand base, String name) {
    return new EditBoatCommand(
        base.ownerId(),
        base.boatId(),
        name,
        base.maxCapacity(),
        base.portName(),
        base.latitude(),
        base.longitude(),
        base.baseRate(),
        base.services(),
        base.photo());
  }

  private static EditBoatCommand with(
      EditBoatCommand base,
      String capacity,
      String portName,
      String latitude,
      String longitude,
      String rate,
      AdditionalServices services,
      byte[] photo) {
    return new EditBoatCommand(
        base.ownerId(),
        base.boatId(),
        base.name(),
        capacity,
        portName,
        latitude,
        longitude,
        rate,
        services,
        photo);
  }

  private static String fieldsOf(ValidationFailedException failure) {
    return failure.violations().stream().map(FieldViolation::field).toList().toString();
  }

  @Test
  void editsTheEditableFieldsAndKeepsTheRest() {
    Vessel vessel = registered("CP-01-0001-A");

    CompletedBoat result = service.edit(command(OWNER, vessel.id()));

    Vessel saved = result.vessel();
    assertEquals("Río Magdalena II", saved.name());
    assertEquals(9, saved.maxCapacity());
    assertEquals(0, new BigDecimal("200000").compareTo(saved.baseRate()));
    assertEquals("CP-01-0001-A", saved.legalRegistration());
    assertEquals(VesselType.MOTORBOAT, saved.type());
    assertEquals(VesselStatus.AVAILABLE, saved.status());
    assertEquals(vessel.registeredAt(), saved.registeredAt());
    assertEquals("https://img.test/old.png", saved.photoPath());
    assertEquals(0, images.calls);
  }

  @Test
  void withoutServicesTheSavedOnesAreKept() {
    Vessel vessel = registered("CP-01-0001-A");

    CompletedBoat result = service.edit(command(OWNER, vessel.id()));

    assertTrue(result.vessel().additionalServices().lifeJackets());
  }

  @Test
  void withServicesTheyReplaceTheSavedOnes() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    CompletedBoat result =
        service.edit(
            with(
                base,
                "9",
                "Marina Central",
                "10.4",
                "-75.5",
                "200000",
                new AdditionalServices(false, true, false, false, true),
                null));

    assertEquals(
        new AdditionalServices(false, true, false, false, true),
        result.vessel().additionalServices());
  }

  @Test
  void aNewPhotoIsStoredAndReplacesTheOldOne() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    CompletedBoat result =
        service.edit(with(base, "9", "Marina Central", "10.4", "-75.5", "200000", null, PNG));

    assertEquals(1, images.calls);
    assertEquals("image/png", images.lastContentType);
    assertEquals("https://res.cloudinary.com/test/image/upload/a.png", result.vessel().photoPath());
  }

  @Test
  void anInvalidPhotoIsRejectedWithoutStoringIt() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () ->
                service.edit(
                    with(
                        base,
                        "9",
                        "Marina Central",
                        "10.4",
                        "-75.5",
                        "200000",
                        null,
                        "no soy imagen".getBytes())));

    assertEquals(RegistrationRules.IMAGE_INVALID_FORMAT, failure.violations().get(0).message());
    assertEquals(0, images.calls);
  }

  @Test
  void imageStorageFailureChangesNothing() {
    Vessel vessel = registered("CP-01-0001-A");
    images.failing = true;
    EditBoatCommand base = command(OWNER, vessel.id());

    assertThrows(
        ExternalServiceUnavailableException.class,
        () ->
            service.edit(with(base, "9", "Marina Central", "10.4", "-75.5", "200000", null, PNG)));

    assertEquals("Río Magdalena I", vessels.store.get(vessel.id()).name());
  }

  @Test
  void aBoatInMaintenanceCanBeEdited() {
    Vessel vessel = withStatus(registered("CP-01-0001-A"), VesselStatus.MAINTENANCE);

    CompletedBoat result = service.edit(command(OWNER, vessel.id()));

    assertEquals(VesselStatus.MAINTENANCE, result.vessel().status());
    assertEquals("Río Magdalena II", result.vessel().name());
  }

  @Test
  void aReservedOrNavigatingBoatIsNotEditedAndReportsItsStatus() {
    for (VesselStatus status :
        new VesselStatus[] {VesselStatus.RESERVED, VesselStatus.NAVIGATION}) {
      Vessel vessel = withStatus(registered("CP-0" + status.ordinal() + "-0001-A"), status);

      BoatNotEditableException failure =
          assertThrows(
              BoatNotEditableException.class, () -> service.edit(command(OWNER, vessel.id())));

      assertEquals(status, failure.currentStatus());
      assertEquals("Río Magdalena I", vessels.store.get(vessel.id()).name());
    }
  }

  @Test
  void anotherOwnersDraftDeletedOrUnknownBoatIsNotFound() {
    Vessel vessel = registered("CP-01-0001-A");
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), OWNER, "Borrador", "CP-09-0009-A", VesselType.YACHT, 10));

    assertThrows(ResourceNotFoundException.class, () -> service.edit(command(OTHER, vessel.id())));
    assertThrows(ResourceNotFoundException.class, () -> service.edit(command(OWNER, draft.id())));
    assertThrows(
        ResourceNotFoundException.class, () -> service.edit(command(OWNER, UUID.randomUUID())));
    vessels.markDeleted(vessel.id(), OWNER, LocalDateTime.now());
    assertThrows(ResourceNotFoundException.class, () -> service.edit(command(OWNER, vessel.id())));
  }

  @Test
  void statusIsCheckedBeforeValidation() {
    Vessel vessel = withStatus(registered("CP-01-0001-A"), VesselStatus.RESERVED);

    assertThrows(
        BoatNotEditableException.class,
        () -> service.edit(withName(command(OWNER, vessel.id()), "")));
  }

  @Test
  void validationReportsEveryFieldAtOnce() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () ->
                service.edit(
                    new EditBoatCommand(
                        OWNER, vessel.id(), "12", "abc", "--", "95", "0", "0", null, null)));

    assertEquals("[name, max_capacity, base_rate, port_name, coordinates]", fieldsOf(failure));
    assertEquals("Río Magdalena I", vessels.store.get(vessel.id()).name());
    assertSame(originalPort, ports.findById(originalPort.id()).orElseThrow());
    assertEquals(base.boatId(), vessel.id());
  }

  @Test
  void capacityAboveTheTypeLimitIsRejected() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () ->
                service.edit(
                    with(base, "11", "Marina Central", "10.4", "-75.5", "200000", null, null)));

    assertEquals(RegistrationRules.CAPACITY_EXCEEDED, failure.violations().get(0).message());
  }

  @Test
  void leadingZerosInCapacityAreAccepted() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    CompletedBoat result =
        service.edit(with(base, "007", "Marina Central", "10.4", "-75.5", "200000", null, null));

    assertEquals(7, result.vessel().maxCapacity());
  }

  @Test
  void namesAreNormalizedCollapsingInnerSpaces() {
    Vessel vessel = registered("CP-01-0001-A");

    CompletedBoat result =
        service.edit(withName(command(OWNER, vessel.id()), "  Río \t  Magdalena   II "));

    assertEquals("Río Magdalena II", result.vessel().name());
  }

  @Test
  void changingOnlyTheCoordinatesUpdatesTheSharedPort() {
    Vessel vessel = registered("CP-01-0001-A");
    Vessel sibling = registered("CP-02-0002-B");
    EditBoatCommand base = command(OWNER, vessel.id());

    CompletedBoat result =
        service.edit(with(base, "9", "Marina Central", "10.45", "-75.55", "200000", null, null));

    assertEquals(originalPort.id(), result.port().id());
    assertEquals(0, new BigDecimal("10.45").compareTo(result.port().latitude()));
    Port seenBySibling = ports.findById(vessels.store.get(sibling.id()).portId()).orElseThrow();
    assertEquals(0, new BigDecimal("-75.55").compareTo(seenBySibling.longitude()));
    assertEquals(1, ports.store.size());
  }

  @Test
  void aNewPortNameCreatesANewPortAndLeavesTheOldOneAlone() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    CompletedBoat result =
        service.edit(with(base, "9", "Puerto Nuevo", "11.1", "-74.2", "200000", null, null));

    assertEquals("Puerto Nuevo", result.port().name());
    assertEquals(2, ports.store.size());
    assertEquals(
        0,
        new BigDecimal("10.4")
            .compareTo(ports.findByName("Marina Central").orElseThrow().latitude()));
  }

  @Test
  void anExistingPortNameIsMatchedIgnoringCase() {
    Vessel vessel = registered("CP-01-0001-A");
    EditBoatCommand base = command(OWNER, vessel.id());

    service.edit(with(base, "9", "marina central", "10.5", "-75.5", "200000", null, null));

    assertEquals(1, ports.store.size());
  }

  @Test
  void retryingTheSameEditIsHarmless() {
    Vessel vessel = registered("CP-01-0001-A");

    CompletedBoat first = service.edit(command(OWNER, vessel.id()));
    CompletedBoat second = service.edit(command(OWNER, vessel.id()));

    assertEquals(first.vessel(), second.vessel());
    assertEquals(1, ports.store.size());
  }
}
