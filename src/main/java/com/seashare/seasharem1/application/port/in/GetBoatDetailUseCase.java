package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Consulta los detalles de una embarcación registrada del propietario. */
public interface GetBoatDetailUseCase {

  /**
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si la embarcación no
   *     existe, es de otro propietario, fue eliminada o sigue en borrador (todas se tratan igual,
   *     para no revelar si el identificador existe)
   */
  CompletedBoat getDetail(UUID ownerId, UUID boatId);
}
