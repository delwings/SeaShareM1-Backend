package com.seashare.seasharem1.infrastructure.adapter.out.persistence;

import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.NotificationPersistenceMapper;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository.NotificationJpaRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Adaptador que implementa el puerto de notificaciones con Spring Data JPA. */
@Component
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

  private final NotificationJpaRepository repository;
  private final NotificationPersistenceMapper mapper;

  public NotificationPersistenceAdapter(
      NotificationJpaRepository repository, NotificationPersistenceMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public Notification save(Notification notification) {
    return mapper.toDomain(repository.save(mapper.toEntity(notification)));
  }

  @Override
  public List<Notification> findLatestByRecipient(UUID recipientId, int limit) {
    return repository
        .findByRecipientIdOrderByCreatedAtDesc(recipientId, PageRequest.of(0, limit))
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countUnread(UUID recipientId) {
    return repository.countByRecipientIdAndReadFalse(recipientId);
  }

  @Override
  public boolean existsUnread(UUID recipientId, UUID vesselId, NotificationType type) {
    return repository.existsByRecipientIdAndVesselIdAndTypeAndReadFalse(
        recipientId, vesselId, type);
  }

  @Override
  public boolean markRead(UUID notificationId, UUID recipientId) {
    return repository.markRead(notificationId.toString(), recipientId.toString()) > 0;
  }

  @Override
  public void markAllRead(UUID recipientId) {
    repository.markAllRead(recipientId.toString());
  }
}
