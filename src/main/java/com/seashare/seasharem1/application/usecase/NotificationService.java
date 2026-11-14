package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.ListNotificationsUseCase;
import com.seashare.seasharem1.application.port.in.MarkNotificationsReadUseCase;
import com.seashare.seasharem1.application.port.in.NotificationList;
import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Notificaciones del propio usuario: lista, contador de no leídas y marcar como leídas. */
public class NotificationService implements ListNotificationsUseCase, MarkNotificationsReadUseCase {

  static final String NOT_FOUND = "Notificación no encontrada.";

  private final NotificationRepositoryPort notifications;

  public NotificationService(NotificationRepositoryPort notifications) {
    this.notifications = notifications;
  }

  @Override
  public NotificationList list(UUID userId) {
    return new NotificationList(
        notifications.findLatestByRecipient(userId, MAX_ITEMS), notifications.countUnread(userId));
  }

  @Override
  public void markRead(UUID userId, UUID notificationId) {
    if (!notifications.markRead(notificationId, userId)) {
      throw new ResourceNotFoundException(NOT_FOUND);
    }
  }

  @Override
  public void markAllRead(UUID userId) {
    notifications.markAllRead(userId);
  }
}
