package com.seashare.seasharem1.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** El propietario envía su embarcación a mantenimiento, sin dar motivo, con la fecha de fin. */
public interface SendToMaintenanceUseCase {

  /**
   * Pasa la embarcación de Disponible a En Mantenimiento/Limpieza. Si ya estaba en mantenimiento
   * devuelve el estado actual sin cambiar nada (reintento idempotente).
   *
   * @param start inicio del mantenimiento; nulo significa «desde ahora»
   * @param until fin del mantenimiento; obligatorio, futuro y posterior al inicio
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException falta el fin, ya
   *     pasó o no es posterior al inicio
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, es de
   *     otro propietario, fue eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException está
   *     Reservada o En Navegación
   */
  CompletedBoat sendToMaintenance(UUID ownerId, UUID boatId, Instant start, Instant until);
}
