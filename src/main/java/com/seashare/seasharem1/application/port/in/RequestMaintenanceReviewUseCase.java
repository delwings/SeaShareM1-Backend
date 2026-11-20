package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/**
 * El propietario avisa al administrador que ya atendió la sanción y pide que revise su embarcación.
 */
public interface RequestMaintenanceReviewUseCase {

  /**
   * Crea un aviso {@code REVIEW_REQUEST} para cada administrador. La embarcación sigue en
   * mantenimiento. Si ya hay un aviso sin leer de esta embarcación, no se duplica.
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException no existe, es de
   *     otro propietario, fue eliminada o es un borrador
   * @throws com.seashare.seasharem1.domain.exception.BoatMaintenanceConflictException no está en
   *     mantenimiento por decisión del administrador (si no, el propietario la termina solo)
   */
  void requestReview(UUID ownerId, UUID boatId);
}
