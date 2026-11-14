package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.ListNotificationsUseCase;
import com.seashare.seasharem1.application.port.in.MarkNotificationsReadUseCase;
import com.seashare.seasharem1.application.port.in.NotificationList;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.NotificationListResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.NotificationResponse;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Campana de notificaciones (propietario y administrador): cada uno ve y marca solo las suyas. */
@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
public class NotificationController {

  private final ListNotificationsUseCase listNotifications;
  private final MarkNotificationsReadUseCase markRead;

  public NotificationController(
      ListNotificationsUseCase listNotifications, MarkNotificationsReadUseCase markRead) {
    this.listNotifications = listNotifications;
    this.markRead = markRead;
  }

  @GetMapping
  public NotificationListResponse list(@AuthenticationPrincipal Jwt jwt) {
    NotificationList result = listNotifications.list(UUID.fromString(jwt.getSubject()));
    return new NotificationListResponse(
        result.notifications().stream().map(NotificationController::toResponse).toList(),
        result.unreadCount());
  }

  @PatchMapping("/{notificationId}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void read(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID notificationId) {
    markRead.markRead(UUID.fromString(jwt.getSubject()), notificationId);
  }

  @PatchMapping("/read-all")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void readAll(@AuthenticationPrincipal Jwt jwt) {
    markRead.markAllRead(UUID.fromString(jwt.getSubject()));
  }

  private static NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.id(),
        notification.vesselId(),
        notification.type().name(),
        notification.message(),
        notification.read(),
        notification.createdAt().toInstant(ZoneOffset.UTC));
  }
}
