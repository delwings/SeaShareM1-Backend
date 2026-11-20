package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.Vessel;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Avisa al propietario de una embarcación. Un fallo al guardar el aviso se registra pero nunca
 * revierte la acción que lo originó.
 */
final class OwnerNotifier {

  private static final Logger LOG = Logger.getLogger(OwnerNotifier.class.getName());
  private static final ZoneId DISPLAY_ZONE = ZoneId.of("America/Bogota");
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

  private final NotificationRepositoryPort notifications;

  OwnerNotifier(NotificationRepositoryPort notifications) {
    this.notifications = notifications;
  }

  /** Fecha en hora de Colombia, con el mes en minúscula: «3 de noviembre de 2026». */
  static String dateText(LocalDateTime utc) {
    return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(DISPLAY_ZONE).format(DATE);
  }

  static void logFailure(Vessel vessel, NotificationType type, RuntimeException failure) {
    LOG.log(Level.WARNING, "No se pudo crear el aviso " + type + " de " + vessel.id(), failure);
  }

  void notify(Vessel vessel, NotificationType type, String message, LocalDateTime now) {
    try {
      notifications.save(
          new Notification(
              UUID.randomUUID(), vessel.ownerId(), vessel.id(), type, message, false, now));
    } catch (RuntimeException failure) {
      logFailure(vessel, type, failure);
    }
  }
}
