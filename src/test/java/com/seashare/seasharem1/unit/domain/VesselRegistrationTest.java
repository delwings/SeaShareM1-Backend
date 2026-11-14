package com.seashare.seasharem1.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VesselRegistrationTest {

  private static final UUID ID = UUID.randomUUID();
  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID PORT = UUID.randomUUID();
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 10, 0);

  private static Vessel draft() {
    return Vessel.newDraft(ID, OWNER, "Río Magdalena II", "CP-05-1042-A", VesselType.YACHT, 15);
  }

  @Test
  void newDraftKeepsOnlyStepOneData() {
    Vessel draft = draft();

    assertEquals(VesselStatus.DRAFT, draft.status());
    assertTrue(draft.isDraft());
    assertEquals(OWNER, draft.ownerId());
    assertNull(draft.photoPath());
    assertNull(draft.baseRate());
    assertNull(draft.portId());
    assertNull(draft.registeredAt());
    assertFalse(draft.deleted());
    assertEquals(AdditionalServices.none(), draft.additionalServices());
  }

  @Test
  void completeMakesItAvailableWithStepTwoData() {
    AdditionalServices services = new AdditionalServices(true, false, true, false, false);

    Vessel completed =
        draft().complete("https://x.test/a.png", services, new BigDecimal("180000"), PORT, NOW);

    assertEquals(VesselStatus.AVAILABLE, completed.status());
    assertEquals(NOW, completed.registeredAt());
    assertEquals(PORT, completed.portId());
    assertEquals("https://x.test/a.png", completed.photoPath());
    assertEquals(new BigDecimal("180000"), completed.baseRate());
    assertEquals(services, completed.additionalServices());
    assertEquals("CP-05-1042-A", completed.legalRegistration());
  }

  @Test
  void onlyADraftCanBeCompleted() {
    Vessel completed = draft().complete("u", AdditionalServices.none(), BigDecimal.ONE, PORT, NOW);

    assertThrows(
        BusinessConflictException.class,
        () -> completed.complete("u", AdditionalServices.none(), BigDecimal.ONE, PORT, NOW));
  }
}
