package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.Vessel;
import java.util.UUID;

/** Consulta el borrador del propietario. */
public interface GetDraftUseCase {

  /**
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si no tiene borrador
   */
  Vessel getDraft(UUID ownerId);
}
