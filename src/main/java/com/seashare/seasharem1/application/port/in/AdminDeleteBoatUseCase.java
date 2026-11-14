package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Eliminación lógica de cualquier embarcación registrada por parte del administrador. */
public interface AdminDeleteBoatUseCase {

  int MAX_REASON = 300;

  /**
   * Elimina la embarcación y avisa a su propietario. Si el aviso falla, la eliminación se conserva.
   *
   * @param reason motivo que verá el propietario; obligatorio, hasta {@link #MAX_REASON} caracteres
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException si el motivo falta o
   *     es muy largo
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si no existe, ya fue
   *     eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatNotDeletableException si su estado actual
   *     no permite eliminarla
   */
  void deleteBoat(UUID adminId, UUID boatId, String reason);
}
