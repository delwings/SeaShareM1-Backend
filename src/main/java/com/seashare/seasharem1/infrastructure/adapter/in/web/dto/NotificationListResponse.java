package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Las notificaciones recientes del usuario y el total sin leer. */
public record NotificationListResponse(
    @JsonProperty("notifications") List<NotificationResponse> notifications,
    @JsonProperty("unread_count") long unreadCount) {}
