package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.application.port.in.GetBoatDetailUseCase;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import java.util.UUID;

/** Detalles de una embarcación registrada; solo el propietario puede verla. */
public class GetBoatDetailService implements GetBoatDetailUseCase {

  static final String NOT_FOUND = "Embarcación no encontrada.";

  private final VesselRepositoryPort vessels;
  private final PortRepositoryPort ports;

  public GetBoatDetailService(VesselRepositoryPort vessels, PortRepositoryPort ports) {
    this.vessels = vessels;
    this.ports = ports;
  }

  @Override
  public CompletedBoat getDetail(UUID ownerId, UUID boatId) {
    Vessel vessel =
        vessels
            .findById(boatId)
            .filter(found -> found.ownerId().equals(ownerId))
            .filter(found -> !found.isDraft())
            .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    Port port = vessel.portId() == null ? null : ports.findById(vessel.portId()).orElse(null);
    return new CompletedBoat(vessel, port);
  }
}
