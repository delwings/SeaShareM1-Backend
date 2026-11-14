package com.seashare.seasharem1.application.port.in;

/** Paso 2 del registro: completa los datos y deja la embarcación Disponible. */
public interface CompleteRegistrationUseCase {

  /**
   * @return la embarcación registrada; si ya estaba registrada (reintento) la devuelve sin repetir
   *     nada
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException el borrador ya no
   *     existe o es de otro propietario
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException datos inválidos
   * @throws com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException falla el
   *     servicio de imágenes
   */
  CompletedBoat complete(CompleteRegistrationCommand command);
}
