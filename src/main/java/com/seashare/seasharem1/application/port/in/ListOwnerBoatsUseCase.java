package com.seashare.seasharem1.application.port.in;

import java.util.List;
import java.util.UUID;

/** Lista las embarcaciones registradas del propietario, la más reciente primero. */
public interface ListOwnerBoatsUseCase {

  List<BoatListItem> list(UUID ownerId);
}
