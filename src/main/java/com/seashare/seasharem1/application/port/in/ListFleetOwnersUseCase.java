package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.User;
import java.util.List;

/** Propietarios que tienen embarcaciones registradas, para el filtro del Administrador. */
public interface ListFleetOwnersUseCase {

  /** Ordenados por nombre. */
  List<User> listOwners();
}
