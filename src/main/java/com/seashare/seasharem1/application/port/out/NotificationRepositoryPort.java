package com.seashare.seasharem1.application.port.out;

import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import java.util.List;
import java.util.UUID;

/** Puerto de salida para guardar y consultar notificaciones. */
public interface NotificationRepositoryPort {

  Notification save(Notification notification);

  /** Las más recientes del destinatario, de la más nueva a la más antigua. */
  List<Notification> findLatestByRecipient(UUID recipientId, int limit);

  long countUnread(UUID recipientId);

  /** Si el destinatario ya tiene sin leer un aviso de ese tipo sobre esa embarcación. */
  boolean existsUnread(UUID recipientId, UUID vesselId, NotificationType type);

  /** Marca como leída una notificación del destinatario; false si no existe o es de otro. */
  boolean markRead(UUID notificationId, UUID recipientId);

  void markAllRead(UUID recipientId);
}
