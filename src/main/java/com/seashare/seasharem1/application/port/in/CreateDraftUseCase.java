package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.Vessel;

/** Paso 1 del registro: guarda los datos básicos y deja la embarcación en estado borrador. */
public interface CreateDraftUseCase {

  /**
   * @return el borrador creado, o el que ya existía si es un reintento con la misma matrícula
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException datos inválidos
   * @throws com.seashare.seasharem1.domain.exception.BusinessConflictException matrícula existente
   *     o el propietario ya tiene otro borrador
   */
  Vessel createDraft(CreateDraftCommand command);
}
