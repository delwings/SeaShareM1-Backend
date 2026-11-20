package com.seashare.seasharem1.application.port.in;

import java.util.List;

/** Listado global de embarcaciones registradas, para el Administrador. */
public interface ListFleetUseCase {

  /** Embarcaciones registradas (sin borradores ni eliminadas), la más reciente primero. */
  List<FleetItem> list(FleetFilter filter);
}
