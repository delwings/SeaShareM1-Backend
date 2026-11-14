package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Marca notificaciones propias como leídas. Repetir la llamada no cambia nada. */
public interface MarkNotificationsReadUseCase {

  /**
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si no existe o es de
   *     otro usuario
   */
  void markRead(UUID userId, UUID notificationId);

  void markAllRead(UUID userId);
}
