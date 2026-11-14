package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Detalles de cualquier embarcación registrada, para el Administrador. */
public interface GetAdminBoatDetailUseCase {

  /**
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, fue
   *     eliminada o es un borrador
   */
  CompletedBoat getDetail(UUID boatId);
}
