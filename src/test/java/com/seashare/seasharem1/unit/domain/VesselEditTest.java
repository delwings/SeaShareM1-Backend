package com.seashare.seasharem1.unit.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.domain.exception.BoatNotEditableException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VesselEditTest {

  private static Vessel available() {
    return Vessel.newDraft(
            UUID.randomUUID(), UUID.randomUUID(), "A1", "CP-01-0001-A", VesselType.YACHT, 10)
        .complete(
            "https://img.test/a.png",
            AdditionalServices.none(),
            new BigDecimal("1000"),
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, 4, 10, 0));
  }

  private static Vessel withStatus(Vessel v, VesselStatus status) {
    return new Vessel(
        v.id(),
        v.name(),
        v.legalRegistration(),
        v.type(),
        v.maxCapacity(),
        v.photoPath(),
        v.hasLifeJackets(),
        v.hasCoolerWithIce(),
        v.hasSoundSystem(),
        v.hasFishingGear(),
        v.hasDivingGear(),
        v.baseRate(),
        status,
        null,
        v.portId(),
        v.ownerId(),
        false,
        v.registeredAt());
  }

  @Test
  void onlyAvailableAndMaintenanceBoatsCanBeEdited() {
    Vessel vessel = available();
    assertTrue(vessel.canBeEdited());
    assertTrue(withStatus(vessel, VesselStatus.MAINTENANCE).canBeEdited());
    assertFalse(withStatus(vessel, VesselStatus.RESERVED).canBeEdited());
    assertFalse(withStatus(vessel, VesselStatus.NAVIGATION).canBeEdited());
    assertFalse(withStatus(vessel, VesselStatus.DRAFT).canBeEdited());
    assertFalse(vessel.asDeleted().canBeEdited());
  }

  @Test
  void editedChangesOnlyTheEditableFields() {
    Vessel vessel = available();
    UUID newPort = UUID.randomUUID();

    Vessel edited =
        vessel.edited(
            "B2",
            12,
            "https://img.test/b.png",
            new AdditionalServices(true, true, false, false, false),
            new BigDecimal("2000"),
            newPort);

    assertEquals("B2", edited.name());
    assertEquals(12, edited.maxCapacity());
    assertEquals(newPort, edited.portId());
    assertTrue(edited.additionalServices().coolerWithIce());
    assertEquals(vessel.id(), edited.id());
    assertEquals(vessel.legalRegistration(), edited.legalRegistration());
    assertEquals(vessel.type(), edited.type());
    assertEquals(vessel.status(), edited.status());
    assertEquals(vessel.ownerId(), edited.ownerId());
    assertEquals(vessel.registeredAt(), edited.registeredAt());
  }

  @Test
  void notEditableExceptionCarriesTheCurrentStatus() {
    BoatNotEditableException exception = new BoatNotEditableException(VesselStatus.RESERVED);
    assertEquals(VesselStatus.RESERVED, exception.currentStatus());
  }

  @Test
  void portComparesLocationByValueNotScale() {
    Port port = new Port(UUID.randomUUID(), "P", new BigDecimal("10.4"), new BigDecimal("-75.50"));
    assertTrue(port.sameLocationAs(new BigDecimal("10.4000"), new BigDecimal("-75.5")));
    assertFalse(port.sameLocationAs(new BigDecimal("10.4001"), new BigDecimal("-75.5")));
  }
}
