package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.CreateDraftCommand;
import com.seashare.seasharem1.application.usecase.CreateDraftService;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateDraftServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER_OWNER = UUID.randomUUID();

  private Fakes.Vessels vessels;
  private CreateDraftService service;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    service = new CreateDraftService(vessels);
  }

  private static CreateDraftCommand command(
      UUID owner, String name, String registration, String type, Integer capacity) {
    return new CreateDraftCommand(owner, name, registration, type, capacity);
  }

  private static List<FieldViolation> violationsOf(ValidationFailedException exception) {
    return exception.violations();
  }

  @Test
  void createsADraftWithNormalizedStepOneData() {
    Vessel draft =
        service.createDraft(command(OWNER, "  Río Magdalena II ", " CP-05-1042-A ", "YACHT", 15));

    assertEquals("Río Magdalena II", draft.name());
    assertEquals("CP-05-1042-A", draft.legalRegistration());
    assertEquals(VesselType.YACHT, draft.type());
    assertEquals(15, draft.maxCapacity());
    assertEquals(VesselStatus.DRAFT, draft.status());
    assertEquals(OWNER, draft.ownerId());
    assertEquals(1, vessels.store.size());
  }

  @Test
  void emptyFormReportsEveryRequiredField() {
    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () -> service.createDraft(command(OWNER, null, null, null, null)));

    assertEquals(
        List.of(
            new FieldViolation("name", RegistrationRules.REQUIRED),
            new FieldViolation("legal_registration", RegistrationRules.REQUIRED),
            new FieldViolation("type", RegistrationRules.REQUIRED),
            new FieldViolation("max_capacity", RegistrationRules.REQUIRED)),
        violationsOf(failure));
    assertTrue(vessels.store.isEmpty());
  }

  @Test
  void capacityAboveTheTypeLimitIsRejected() {
    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () ->
                service.createDraft(command(OWNER, "Lancha Uno", "CP-05-1042-A", "MOTORBOAT", 11)));

    assertEquals(
        List.of(new FieldViolation("max_capacity", RegistrationRules.CAPACITY_EXCEEDED)),
        violationsOf(failure));
  }

  @Test
  void unknownTypeIsRejected() {
    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () -> service.createDraft(command(OWNER, "Lancha Uno", "CP-05-1042-A", "BARGE", 5)));

    assertEquals(
        List.of(new FieldViolation("type", RegistrationRules.TYPE_INVALID)), violationsOf(failure));
  }

  @Test
  void capacityWithoutTypeOnlyComplainsAboutTheType() {
    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () -> service.createDraft(command(OWNER, "Lancha Uno", "CP-05-1042-A", null, 500)));

    assertEquals(
        List.of(new FieldViolation("type", RegistrationRules.REQUIRED)), violationsOf(failure));
  }

  @Test
  void takenRegistrationIsAConflictShownUnderTheField() {
    service.createDraft(command(OTHER_OWNER, "Otra", "CP-05-1042-A", "YACHT", 10));

    BusinessConflictException conflict =
        assertThrows(
            BusinessConflictException.class,
            () -> service.createDraft(command(OWNER, "Mia", "CP-05-1042-A", "YACHT", 10)));

    assertEquals(
        List.of(
            new FieldViolation("legal_registration", RegistrationRules.LEGAL_REGISTRATION_TAKEN)),
        conflict.violations());
    assertEquals(1, vessels.store.size());
  }

  @Test
  void badFormatIsReportedBeforeCheckingIfItExists() {
    service.createDraft(command(OTHER_OWNER, "Otra", "CP-05-1042-A", "YACHT", 10));

    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () -> service.createDraft(command(OWNER, "Mia", "CP-5-1042-A", "YACHT", 10)));

    assertEquals(
        List.of(
            new FieldViolation("legal_registration", RegistrationRules.LEGAL_REGISTRATION_INVALID)),
        violationsOf(failure));
  }

  @Test
  void anOwnerWithADraftCannotStartAnother() {
    service.createDraft(command(OWNER, "Primera", "CP-05-1042-A", "YACHT", 10));

    BusinessConflictException conflict =
        assertThrows(
            BusinessConflictException.class,
            () -> service.createDraft(command(OWNER, "Segunda", "CP-05-2000-B", "YACHT", 10)));

    assertTrue(conflict.violations().isEmpty());
    assertEquals(1, vessels.store.size());
  }

  @Test
  void retryingTheSameDraftDoesNotCreateADuplicate() {
    Vessel first = service.createDraft(command(OWNER, "Primera", "CP-05-1042-A", "YACHT", 10));

    Vessel retry = service.createDraft(command(OWNER, "Primera", "CP-05-1042-A", "YACHT", 10));

    assertSame(first, retry);
    assertEquals(1, vessels.store.size());
  }

  @Test
  void eachOwnerHasHisOwnDraft() {
    service.createDraft(command(OWNER, "Primera", "CP-05-1042-A", "YACHT", 10));
    Vessel other =
        service.createDraft(command(OTHER_OWNER, "Segunda", "CP-05-2000-B", "SAILBOAT", 12));

    assertEquals(OTHER_OWNER, other.ownerId());
    assertEquals(2, vessels.store.size());
    assertNull(other.registeredAt());
  }
}
