package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/** Consulta las notificaciones del propio usuario. */
public interface ListNotificationsUseCase {

  /** Máximo de notificaciones que devuelve la lista. */
  int MAX_ITEMS = 50;

  NotificationList list(UUID userId);
}
