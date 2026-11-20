package com.seashare.seasharem1.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Una notificación; `created_at` va en UTC. */
public record NotificationResponse(
    @JsonProperty("notification_id") UUID notificationId,
    @JsonProperty("vessel_id") UUID vesselId,
    @JsonProperty("type") String type,
    @JsonProperty("message") String message,
    @JsonProperty("read") boolean read,
    @JsonProperty("created_at") Instant createdAt) {}
