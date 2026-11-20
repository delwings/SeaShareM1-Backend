package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.Notification;
import java.util.List;

/** Las notificaciones más recientes de un usuario y cuántas siguen sin leer. */
public record NotificationList(List<Notification> notifications, long unreadCount) {}
