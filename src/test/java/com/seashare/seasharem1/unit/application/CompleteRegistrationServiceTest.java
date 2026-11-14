package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.CompleteRegistrationCommand;
import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.usecase.CompleteRegistrationService;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
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
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteRegistrationServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER_OWNER = UUID.randomUUID();
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);
  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};

  private Fakes.Vessels vessels;
  private Fakes.Ports ports;
  private Fakes.Images images;
  private CompleteRegistrationService service;
  private Vessel draft;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    ports = new Fakes.Ports();
    images = new Fakes.Images();
    service = new CompleteRegistrationService(vessels, ports, images, CLOCK);
    draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                OWNER,
                "Río Magdalena II",
                "CP-05-1042-A",
                VesselType.YACHT,
                15));
  }

  private CompleteRegistrationCommand valid(UUID boatId) {
    return new CompleteRegistrationCommand(
        OWNER,
        boatId,
        " Marina Magdalena ",
        "11.24083",
        "-74.199",
        "0180000",
        new AdditionalServices(true, false, false, true, false),
        PNG);
  }

  @Test
  void completesTheRegistrationAndLeavesTheBoatAvailable() {
    CompletedBoat result = service.complete(valid(draft.id()));

    Vessel vessel = result.vessel();
    assertEquals(VesselStatus.AVAILABLE, vessel.status());
    assertEquals(LocalDateTime.of(2026, 11, 4, 15, 0), vessel.registeredAt());
    assertEquals(new BigDecimal("180000"), vessel.baseRate());
    assertEquals("https://res.cloudinary.com/test/image/upload/a.png", vessel.photoPath());
    assertEquals(
        new AdditionalServices(true, false, false, true, false), vessel.additionalServices());
    assertEquals(result.port().id(), vessel.portId());
    assertEquals("Marina Magdalena", result.port().name());
    assertEquals(new BigDecimal("11.2408"), result.port().latitude());
    assertEquals(new BigDecimal("-74.1990"), result.port().longitude());
    assertEquals("image/png", images.lastContentType);
    assertEquals("seashare/boats", images.lastFolder);
    assertEquals(VesselStatus.AVAILABLE, vessels.store.get(draft.id()).status());
  }

  @Test
  void reusesAnExistingPortWithTheSameName() {
    Port existing =
        ports.save(
            new Port(
                UUID.randomUUID(),
                "MARINA MAGDALENA",
                new BigDecimal("1.0000"),
                new BigDecimal("2.0000")));

    CompletedBoat result = service.complete(valid(draft.id()));

    assertEquals(existing.id(), result.port().id());
    assertEquals(new BigDecimal("1.0000"), result.port().latitude());
    assertEquals(1, ports.store.size());
  }

  @Test
  void aMissingDraftIsNotFound() {
    assertThrows(ResourceNotFoundException.class, () -> service.complete(valid(UUID.randomUUID())));
  }

  @Test
  void anotherOwnersDraftLooksLikeItDoesNotExist() {
    CompleteRegistrationCommand command =
        new CompleteRegistrationCommand(
            OTHER_OWNER, draft.id(), "Marina", "1", "2", "100", AdditionalServices.none(), PNG);

    assertThrows(ResourceNotFoundException.class, () -> service.complete(command));
  }

  @Test
  void retryingAnAlreadyCompletedRegistrationChangesNothing() {
    CompletedBoat first = service.complete(valid(draft.id()));

    CompletedBoat retry = service.complete(valid(draft.id()));

    assertEquals(first.vessel(), retry.vessel());
    assertEquals(first.port(), retry.port());
    assertEquals(1, images.calls);
    assertEquals(1, ports.store.size());
  }

  @Test
  void aBoatInAnotherStateIsAConflict() {
    Vessel inMaintenance =
        new Vessel(
            UUID.randomUUID(),
            "Otra",
            "CP-05-9999-Z",
            VesselType.YACHT,
            10,
            "u",
            true,
            true,
            true,
            true,
            true,
            BigDecimal.TEN,
            VesselStatus.MAINTENANCE,
            null,
            UUID.randomUUID(),
            OWNER,
            false,
            LocalDateTime.of(2026, 1, 1, 0, 0));
    vessels.store.put(inMaintenance.id(), inMaintenance);

    assertThrows(
        BusinessConflictException.class, () -> service.complete(valid(inMaintenance.id())));
  }

  @Test
  void emptyStepTwoReportsEveryRequiredFieldAndTouchesNothing() {
    CompleteRegistrationCommand empty =
        new CompleteRegistrationCommand(
            OWNER, draft.id(), null, null, null, null, AdditionalServices.none(), null);

    ValidationFailedException failure =
        assertThrows(ValidationFailedException.class, () -> service.complete(empty));

    assertEquals(
        List.of(
            new FieldViolation("port_name", RegistrationRules.REQUIRED),
            new FieldViolation("coordinates", RegistrationRules.COORDINATES_REQUIRED),
            new FieldViolation("base_rate", RegistrationRules.REQUIRED),
            new FieldViolation("photo", RegistrationRules.REQUIRED)),
        failure.violations());
    assertEquals(0, images.calls);
    assertTrue(ports.store.isEmpty());
    assertEquals(VesselStatus.DRAFT, vessels.store.get(draft.id()).status());
  }

  @Test
  void invalidValuesGiveTheSpecMessages() {
    CompleteRegistrationCommand invalid =
        new CompleteRegistrationCommand(
            OWNER,
            draft.id(),
            "12",
            "abc",
            "-74.199",
            "12.5",
            AdditionalServices.none(),
            "no soy una imagen".getBytes());

    ValidationFailedException failure =
        assertThrows(ValidationFailedException.class, () -> service.complete(invalid));

    assertEquals(
        List.of(
            new FieldViolation("port_name", RegistrationRules.PORT_NAME_INVALID),
            new FieldViolation("coordinates", RegistrationRules.COORDINATES_INVALID),
            new FieldViolation("base_rate", RegistrationRules.NO_DECIMALS),
            new FieldViolation("photo", RegistrationRules.IMAGE_INVALID_FORMAT)),
        failure.violations());
  }

  @Test
  void aFailureInTheImageServiceLeavesTheDraftUntouched() {
    images.failing = true;

    assertThrows(
        ExternalServiceUnavailableException.class, () -> service.complete(valid(draft.id())));

    assertEquals(VesselStatus.DRAFT, vessels.store.get(draft.id()).status());
  }
}
