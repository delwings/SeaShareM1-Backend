package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.DeleteDraftUseCase;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import java.util.UUID;

/** Elimina de forma permanente el borrador del propietario, y su matrícula queda libre. */
public class DeleteDraftService implements DeleteDraftUseCase {

  private final VesselRepositoryPort vessels;

  public DeleteDraftService(VesselRepositoryPort vessels) {
    this.vessels = vessels;
  }

  @Override
  public void deleteDraft(UUID ownerId) {
    vessels.findDraftByOwner(ownerId).ifPresent(draft -> vessels.deleteById(draft.id()));
  }
}
