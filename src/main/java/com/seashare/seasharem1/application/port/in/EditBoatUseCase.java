package com.seashare.seasharem1.application.port.in;

/** Edición de la información de una embarcación registrada por su propietario. */
public interface EditBoatUseCase {

  /**
   * Verifica en orden: existencia y propiedad, estado, validación de campos y, por último, guarda.
   *
   * @return la embarcación con sus datos ya actualizados
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, es de
   *     otro propietario, fue eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatNotEditableException el estado no permite
   *     editar
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException datos inválidos
   * @throws com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException falla el
   *     servicio de imágenes
   */
  CompletedBoat edit(EditBoatCommand command);
}
