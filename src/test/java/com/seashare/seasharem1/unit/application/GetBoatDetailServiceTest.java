package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.usecase.GetBoatDetailService;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetBoatDetailServiceTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private Fakes.Vessels vessels;
  private Fakes.Ports ports;
  private GetBoatDetailService service;
  private Port port;

  @BeforeEach
  void setUp() {
    vessels = new Fakes.Vessels();
    ports = new Fakes.Ports();
    service = new GetBoatDetailService(vessels, ports);
    port =
        ports.save(
            new Port(
                UUID.randomUUID(),
                "Marina Magdalena",
                new BigDecimal("11.2408"),
                new BigDecimal("-74.1990")));
  }

  private Vessel registered() {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(),
                OWNER,
                "Río Magdalena I",
                "CP-05-1042-A",
                VesselType.MOTORBOAT,
                8));
    return vessels.save(
        draft.complete(
            "https://img.test/a.png",
            new AdditionalServices(true, false, false, false, true),
            new BigDecimal("180000"),
            port.id(),
            LocalDateTime.of(2026, 11, 4, 10, 0)));
  }

  @Test
  void returnsTheBoatAndItsPortToItsOwner() {
    Vessel vessel = registered();

    CompletedBoat result = service.getDetail(OWNER, vessel.id());

    assertEquals(vessel, result.vessel());
    assertEquals(port, result.port());
  }

  @Test
  void anotherOwnersBoatLooksLikeItDoesNotExist() {
    Vessel vessel = registered();

    ResourceNotFoundException error =
        assertThrows(ResourceNotFoundException.class, () -> service.getDetail(OTHER, vessel.id()));

    assertEquals("Embarcación no encontrada.", error.getMessage());
  }

  @Test
  void unknownIdIsNotFound() {
    assertThrows(
        ResourceNotFoundException.class, () -> service.getDetail(OWNER, UUID.randomUUID()));
  }

  @Test
  void aDraftIsTreatedAsNonExistent() {
    Vessel draft =
        vessels.save(
            Vessel.newDraft(
                UUID.randomUUID(), OWNER, "Borrador", "CP-01-0001-A", VesselType.YACHT, 10));

    assertThrows(ResourceNotFoundException.class, () -> service.getDetail(OWNER, draft.id()));
  }

  @Test
  void aMissingPortDoesNotBreakTheDetail() {
    Vessel vessel = registered();
    ports.store.clear();

    assertNull(service.getDetail(OWNER, vessel.id()).port());
  }
}
