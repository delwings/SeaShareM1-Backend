package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Eliminación lógica de una embarcación registrada del propietario. */
public interface DeleteBoatUseCase {

  /**
   * Elimina la embarcación. Su matrícula queda libre y el registro se conserva como historial.
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si no existe, ya fue
   *     eliminada, es un borrador o es de otro propietario
   * @throws com.seashare.seasharem1.domain.exception.BoatNotDeletableException si su estado actual
   *     no permite eliminarla
   */
  void deleteBoat(UUID ownerId, UUID boatId);
}
