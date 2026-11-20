package com.seashare.seasharem1.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Notificación sobre una embarcación dirigida a un usuario.
 *
 * @param read si el destinatario ya la leyó
 */
public record Notification(
    UUID id,
    UUID recipientId,
    UUID vesselId,
    NotificationType type,
    String message,
    boolean read,
    LocalDateTime createdAt) {}
