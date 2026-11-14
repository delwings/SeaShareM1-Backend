package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.BoatListItem;
import com.seashare.seasharem1.application.usecase.CheckLegalRegistrationService;
import com.seashare.seasharem1.application.usecase.DeleteDraftService;
import com.seashare.seasharem1.application.usecase.GetDraftService;
import com.seashare.seasharem1.application.usecase.ListOwnerBoatsService;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Vessel;
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

class OwnerBoatsServicesTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER_OWNER = UUID.randomUUID();
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 15, 0);
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-04T15:00:00Z"), ZoneOffset.UTC);

  private Fakes.Vessels vessels;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
  }

  private Vessel registered(String name, String registration, UUID owner, LocalDateTime at) {
    Vessel draft =
        Vessel.newDraft(UUID.randomUUID(), owner, name, registration, VesselType.YACHT, 10);
    return vessels.save(
        draft.complete("u", AdditionalServices.none(), BigDecimal.TEN, UUID.randomUUID(), at));
  }

  private Vessel draftOf(UUID owner, String registration) {
    return vessels.save(
        Vessel.newDraft(UUID.randomUUID(), owner, "Borrador", registration, VesselType.YACHT, 10));
  }

  // ---- lista

  @Test
  void listsNewestFirstAndOnlyTheNewestIsTaggedNew() {
    registered("Vieja", "CP-01-0001-A", OWNER, NOW.minusDays(30));
    registered("Reciente", "CP-01-0002-A", OWNER, NOW.minusHours(2));
    registered("Media", "CP-01-0003-A", OWNER, NOW.minusDays(3));
    ListOwnerBoatsService service = new ListOwnerBoatsService(vessels, CLOCK);

    List<BoatListItem> items = service.list(OWNER);

    assertEquals(
        List.of("Reciente", "Media", "Vieja"), items.stream().map(i -> i.vessel().name()).toList());
    assertEquals(List.of(true, false, false), items.stream().map(BoatListItem::isNew).toList());
  }

  @Test
  void theNewTagExpiresAfterExactly168Hours() {
    registered("Casi", "CP-01-0001-A", OWNER, NOW.minusHours(168).plusSeconds(1));
    assertTrue(new ListOwnerBoatsService(vessels, CLOCK).list(OWNER).get(0).isNew());

    Fakes.Vessels other = new Fakes.Vessels();
    Vessel exact =
        Vessel.newDraft(UUID.randomUUID(), OWNER, "Exacta", "CP-01-0009-A", VesselType.YACHT, 10)
            .complete(
                "u",
                AdditionalServices.none(),
                BigDecimal.TEN,
                UUID.randomUUID(),
                NOW.minusHours(168));
    other.save(exact);
    assertFalse(new ListOwnerBoatsService(other, CLOCK).list(OWNER).get(0).isNew());
  }

  @Test
  void listIgnoresDraftsAndOtherOwners() {
    draftOf(OWNER, "CP-01-0001-A");
    registered("Ajena", "CP-01-0002-A", OTHER_OWNER, NOW.minusDays(1));

    assertTrue(new ListOwnerBoatsService(vessels, CLOCK).list(OWNER).isEmpty());
  }

  // ---- borrador

  @Test
  void getDraftReturnsTheOwnersDraft() {
    Vessel draft = draftOf(OWNER, "CP-01-0001-A");

    assertEquals(draft, new GetDraftService(vessels).getDraft(OWNER));
  }

  @Test
  void getDraftWithoutDraftIsNotFound() {
    assertThrows(
        ResourceNotFoundException.class, () -> new GetDraftService(vessels).getDraft(OWNER));
  }

  @Test
  void deleteDraftRemovesItAndFreesTheRegistration() {
    draftOf(OWNER, "CP-01-0001-A");
    DeleteDraftService service = new DeleteDraftService(vessels);

    service.deleteDraft(OWNER);

    assertTrue(vessels.store.isEmpty());
    assertFalse(vessels.existsByLegalRegistration("CP-01-0001-A"));
  }

  @Test
  void deleteDraftWithoutDraftDoesNothingAndKeepsOtherOwnersDrafts() {
    draftOf(OTHER_OWNER, "CP-01-0002-A");

    new DeleteDraftService(vessels).deleteDraft(OWNER);

    assertEquals(1, vessels.store.size());
  }

  // ---- disponibilidad de la matrícula

  @Test
  void availabilityIsTrueForAFreeRegistrationAndFalseForATakenOne() {
    draftOf(OTHER_OWNER, "CP-01-0001-A");
    CheckLegalRegistrationService service = new CheckLegalRegistrationService(vessels);

    assertTrue(service.isAvailable(" CP-01-0002-A "));
    assertFalse(service.isAvailable("CP-01-0001-A"));
  }

  @Test
  void availabilityRejectsABadFormat() {
    ValidationFailedException failure =
        assertThrows(
            ValidationFailedException.class,
            () -> new CheckLegalRegistrationService(vessels).isAvailable("cp-5-1-a"));

    assertEquals(
        List.of(
            new FieldViolation("legal_registration", RegistrationRules.LEGAL_REGISTRATION_INVALID)),
        failure.violations());
  }
}
