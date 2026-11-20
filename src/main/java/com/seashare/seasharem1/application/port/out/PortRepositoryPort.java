package com.seashare.seasharem1.application.port.out;

import com.seashare.seasharem1.domain.model.Port;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para guardar y consultar puertos de atraque. */
public interface PortRepositoryPort {

  /** Busca por nombre sin distinguir mayúsculas ni tildes. */
  Optional<Port> findByName(String name);

  Optional<Port> findById(UUID id);

  /** Guarda el puerto; si otro guardado simultáneo creó el mismo nombre, devuelve ese. */
  Port save(Port port);
}
