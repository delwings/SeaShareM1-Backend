package com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository;

import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.NotificationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Repositorio Spring Data de notificaciones. Solo lo usa el adaptador de persistencia. */
public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

  List<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable page);

  long countByRecipientIdAndReadFalse(UUID recipientId);

  boolean existsByRecipientIdAndVesselIdAndTypeAndReadFalse(
      UUID recipientId, UUID vesselId, NotificationType type);

  /** Marca como leída solo si es del destinatario; repetirla es inofensivo. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = "UPDATE notification SET is_read = TRUE WHERE id = :id AND recipient_id = :recipient",
      nativeQuery = true)
  int markRead(@Param("id") String id, @Param("recipient") String recipient);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE notification SET is_read = TRUE WHERE recipient_id = :recipient AND is_read ="
              + " FALSE",
      nativeQuery = true)
  int markAllRead(@Param("recipient") String recipient);
}
