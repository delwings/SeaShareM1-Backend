package com.seashare.seasharem1.application.port.in;

/**
 * Asignar estado operativo: aplica un aviso del Módulo 2 (reservas) al estado de una embarcación.
 * Lo llaman por igual el simulador REST y, más adelante, el consumidor de la cola.
 */
public interface AssignOperationalStatusUseCase {

  /**
   * @param event aviso tal como llega (texto), para validar su formato
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException si el aviso viene
   *     incompleto o con valores inválidos
   */
  Outcome assign(StatusEvent event);

  /**
   * Aviso de M2. Solo se leen estos cuatro datos; cualquier otro (por ejemplo montos) se ignora.
   *
   * @param eventId identificador único del aviso
   * @param vesselId embarcación afectada (UUID)
   * @param operationalStatus nuevo estado: RESERVED, IN_NAVIGATION o NAVIGATION,
   *     MAINTENANCE_CLEANING o MAINTENANCE, AVAILABLE
   * @param occurredAt fecha y hora exacta del suceso (ISO 8601 con zona)
   */
  record StatusEvent(
      String eventId, String vesselId, String operationalStatus, String occurredAt) {}

  /** Qué se hizo con el aviso. Todos se confirman al emisor; solo APPLIED cambia el estado. */
  enum Outcome {
    APPLIED,
    DUPLICATE,
    STALE,
    UNKNOWN_VESSEL,
    INVALID_TRANSITION,
    NO_CHANGE
  }
}
