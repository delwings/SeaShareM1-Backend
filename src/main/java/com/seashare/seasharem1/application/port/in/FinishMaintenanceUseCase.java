package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** El propietario termina el mantenimiento de su embarcación y la deja Disponible. */
public interface FinishMaintenanceUseCase {

  /**
   * Pasa la embarcación de En Mantenimiento/Limpieza a Disponible. Si ya estaba Disponible devuelve
   * el estado actual sin cambiar nada (reintento idempotente).
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, es de
   *     otro propietario, fue eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException la suspendió
   *     el administrador, o está Reservada o En Navegación
   */
  CompletedBoat finishMaintenance(UUID ownerId, UUID boatId);
}
