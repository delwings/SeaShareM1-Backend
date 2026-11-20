package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Elimina de forma permanente el borrador del propietario. Si no tiene, no hace nada. */
public interface DeleteDraftUseCase {

  void deleteDraft(UUID ownerId);
}
