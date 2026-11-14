package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** El propietario envía su embarcación a mantenimiento, sin dar motivo. */
public interface SendToMaintenanceUseCase {

  /**
   * Pasa la embarcación de Disponible a En Mantenimiento/Limpieza. Si ya estaba en mantenimiento
   * devuelve el estado actual sin cambiar nada (reintento idempotente).
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, es de
   *     otro propietario, fue eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException está
   *     Reservada o En Navegación
   */
  CompletedBoat sendToMaintenance(UUID ownerId, UUID boatId);
}
