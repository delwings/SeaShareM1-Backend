package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.BaseRateQuery.BaseRates;
import com.seashare.seasharem1.application.usecase.BaseRateQueryService;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaseRateQueryServiceTest {

  private Fakes.Vessels vessels;
  private BaseRateQueryService service;
  private int sequence;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    service = new BaseRateQueryService(vessels);
  }

  private Vessel registered(String rate) {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Barco",
                "CP-12-" + (700 + sequence++) + "-B",
                VesselType.YACHT,
                10));
    return vessels.save(
        draft.complete(
            "https://img.test/a.png",
            AdditionalServices.none(),
            new BigDecimal(rate),
            UUID.randomUUID(),
            LocalDateTime.of(2026, 11, 4, 10, 0)));
  }

  private Vessel withStatus(Vessel v, VesselStatus status) {
    return vessels.save(
        new Vessel(
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
            v.maintenanceBy(),
            v.portId(),
            v.ownerId(),
            v.deleted(),
            v.registeredAt()));
  }

  private static List<String> ids(Vessel... boats) {
    List<String> out = new ArrayList<>();
    for (Vessel b : boats) out.add(b.id().toString());
    return out;
  }

  @Test
  void returnsEachRateWithItsIdInRequestOrder() {
    Vessel a = registered("350000");
    Vessel b = registered("180000");
    Vessel c = registered("90000");

    BaseRates result = service.getBaseRates(ids(c, a, b));

    assertEquals(
        List.of(c.id(), a.id(), b.id()), result.rates().stream().map(r -> r.boatId()).toList());
    assertEquals(new BigDecimal("90000"), result.rates().get(0).baseRate());
    assertTrue(result.notFound().isEmpty());
  }

  @Test
  void aSingleBoatBatchWorksTheSame() {
    Vessel a = registered("350000");

    BaseRates result = service.getBaseRates(ids(a));

    assertEquals(1, result.rates().size());
  }

  @Test
  void everyOperationalStateReturnsItsRate() {
    List<Vessel> boats = new ArrayList<>();
    for (VesselStatus status :
        List.of(
            VesselStatus.AVAILABLE,
            VesselStatus.RESERVED,
            VesselStatus.NAVIGATION,
            VesselStatus.MAINTENANCE)) {
      boats.add(withStatus(registered("150000"), status));
    }

    BaseRates result = service.getBaseRates(ids(boats.toArray(new Vessel[0])));

    assertEquals(4, result.rates().size());
    assertTrue(result.notFound().isEmpty());
  }

  @Test
  void missingDeletedAndDraftBoatsAreReportedApartWithoutAffectingTheRest() {
    Vessel ok = registered("350000");
    Vessel deleted = registered("100000");
    vessels.store.put(deleted.id(), deleted.asDeleted());
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Borrador",
                "CP-12-999-B",
                VesselType.YACHT,
                8));
    UUID unknown = UUID.randomUUID();

    BaseRates result =
        service.getBaseRates(
            List.of(
                ok.id().toString(),
                deleted.id().toString(),
                draft.id().toString(),
                unknown.toString()));

    assertEquals(1, result.rates().size());
    assertEquals(List.of(deleted.id(), draft.id(), unknown), result.notFound());
  }

  @Test
  void aBatchWithNoFoundBoatReturnsEmptyRates() {
    UUID unknown = UUID.randomUUID();

    BaseRates result = service.getBaseRates(List.of(unknown.toString()));

    assertTrue(result.rates().isEmpty());
    assertEquals(List.of(unknown), result.notFound());
  }

  @Test
  void aRepeatedIdIsDeliveredOnce() {
    Vessel a = registered("350000");

    BaseRates result =
        service.getBaseRates(List.of(a.id().toString(), a.id().toString().toUpperCase()));

    assertEquals(1, result.rates().size());
  }

  @Test
  void anEditedRateIsReadFreshOnEveryQuery() {
    Vessel a = registered("150000");
    service.getBaseRates(ids(a));
    vessels.store.put(
        a.id(),
        new Vessel(
            a.id(),
            a.name(),
            a.legalRegistration(),
            a.type(),
            a.maxCapacity(),
            a.photoPath(),
            a.hasLifeJackets(),
            a.hasCoolerWithIce(),
            a.hasSoundSystem(),
            a.hasFishingGear(),
            a.hasDivingGear(),
            new BigDecimal("180000"),
            a.status(),
            a.maintenanceBy(),
            a.portId(),
            a.ownerId(),
            a.deleted(),
            a.registeredAt()));

    assertEquals(new BigDecimal("180000"), service.getBaseRates(ids(a)).rates().get(0).baseRate());
  }

  @Test
  void rejectsNullEmptyOversizedAndMalformedBatches() {
    assertThrows(ValidationFailedException.class, () -> service.getBaseRates(null));
    assertThrows(ValidationFailedException.class, () -> service.getBaseRates(List.of()));

    List<String> tooMany = new ArrayList<>();
    for (int i = 0; i < 101; i++) tooMany.add(UUID.randomUUID().toString());
    assertThrows(ValidationFailedException.class, () -> service.getBaseRates(tooMany));

    List<String> hundred = tooMany.subList(0, 100);
    assertEquals(100, service.getBaseRates(hundred).notFound().size());

    ValidationFailedException bad =
        assertThrows(
            ValidationFailedException.class,
            () -> service.getBaseRates(List.of(UUID.randomUUID().toString(), "abc", "")));
    assertEquals(
        List.of("boat_ids[1]", "boat_ids[2]"),
        bad.violations().stream().map(v -> v.field()).toList());
  }
}
