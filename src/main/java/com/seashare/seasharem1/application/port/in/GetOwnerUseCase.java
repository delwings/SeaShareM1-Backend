package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.User;
import java.util.UUID;

/** Información de un propietario, para el Administrador. Solo lectura. */
public interface GetOwnerUseCase {

  /**
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe o el
   *     usuario no es propietario
   */
  User getOwner(UUID ownerId);
}
