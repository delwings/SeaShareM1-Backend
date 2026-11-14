package com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity;

import com.seashare.seasharem1.domain.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Entidad JPA de la tabla notification. */
@Entity
@Table(name = "notification")
public class NotificationEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", length = 36, nullable = false, updatable = false)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "recipient_id", length = 36, nullable = false)
  private UUID recipientId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "vessel_id", length = 36, nullable = false)
  private UUID vesselId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private NotificationType type;

  @Column(name = "message", nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(UUID recipientId) {
    this.recipientId = recipientId;
  }

  public UUID getVesselId() {
    return vesselId;
  }

  public void setVesselId(UUID vesselId) {
    this.vesselId = vesselId;
  }

  public NotificationType getType() {
    return type;
  }

  public void setType(NotificationType type) {
    this.type = type;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
