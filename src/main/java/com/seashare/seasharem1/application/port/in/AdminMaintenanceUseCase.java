package com.seashare.seasharem1.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Mantenimiento por parte del administrador, sobre cualquier embarcación registrada. Solo termina
 * el mantenimiento que él mismo inició; el del propietario lo termina el propietario.
 */
public interface AdminMaintenanceUseCase {

  /**
   * Pasa de Disponible a En Mantenimiento/Limpieza, con motivo obligatorio, y avisa al propietario.
   * Si ya estaba en mantenimiento devuelve el estado actual sin cambiar ni avisar de nuevo.
   *
   * @param start inicio del mantenimiento; nulo significa «desde ahora»
   * @param until fin del mantenimiento; obligatorio, futuro y posterior al inicio
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException motivo vacío o de
   *     más de 300 caracteres, o fechas inválidas
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, fue
   *     eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException está
   *     Reservada o En Navegación
   */
  CompletedBoat sendToMaintenance(
      UUID adminId, UUID boatId, String reason, Instant start, Instant until);

  /**
   * Pasa de mantenimiento a Disponible y avisa al propietario. Si ya estaba Disponible devuelve el
   * estado actual sin cambiar ni avisar.
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, fue
   *     eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException el
   *     mantenimiento no lo inició el administrador, o la embarcación no está en mantenimiento
   */
  CompletedBoat finishMaintenance(UUID adminId, UUID boatId);
}
