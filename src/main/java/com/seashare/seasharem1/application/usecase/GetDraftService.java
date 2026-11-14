package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.GetDraftUseCase;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Vessel;
import java.util.UUID;

/** Consulta el borrador del propietario. */
public class GetDraftService implements GetDraftUseCase {

  private final VesselRepositoryPort vessels;

  public GetDraftService(VesselRepositoryPort vessels) {
    this.vessels = vessels;
  }

  @Override
  public Vessel getDraft(UUID ownerId) {
    return vessels
        .findDraftByOwner(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException("No tienes un borrador de registro."));
  }
}
