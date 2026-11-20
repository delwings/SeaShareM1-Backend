package com.seashare.seasharem1.unit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.seashare.seasharem1.application.port.in.NotificationList;
import com.seashare.seasharem1.application.usecase.NotificationService;
import com.seashare.seasharem1.domain.exception.ResourceNotFoundException;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

  private static final UUID ME = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private Fakes.Notifications store;
  private NotificationService service;

  @BeforeEach
  void setUp() {
    store = new Fakes.Notifications();
    service = new NotificationService(store);
  }

  private Notification add(UUID recipient, int minute) {
    return store.save(
        new Notification(
            UUID.randomUUID(),
            recipient,
            UUID.randomUUID(),
            NotificationType.VESSEL_DELETED,
            "mensaje " + minute,
            false,
            LocalDateTime.of(2026, 11, 3, 10, minute)));
  }

  @Test
  void listsOnlyMyNotificationsNewestFirstWithTheUnreadCount() {
    add(ME, 1);
    Notification newest = add(ME, 5);
    add(OTHER, 3);

    NotificationList list = service.list(ME);

    assertEquals(2, list.notifications().size());
    assertEquals(newest.id(), list.notifications().get(0).id());
    assertEquals(2, list.unreadCount());
  }

  @Test
  void markingOneReadLowersTheCountAndRepeatingItIsHarmless() {
    Notification mine = add(ME, 1);
    add(ME, 2);

    service.markRead(ME, mine.id());
    service.markRead(ME, mine.id());

    assertEquals(1, service.list(ME).unreadCount());
  }

  @Test
  void anotherUsersOrUnknownNotificationIsNotFound() {
    Notification theirs = add(OTHER, 1);

    assertThrows(ResourceNotFoundException.class, () -> service.markRead(ME, theirs.id()));
    assertThrows(ResourceNotFoundException.class, () -> service.markRead(ME, UUID.randomUUID()));
    assertFalse(store.store.get(0).read());
  }

  @Test
  void markAllReadOnlyAffectsMine() {
    add(ME, 1);
    add(ME, 2);
    add(OTHER, 3);

    service.markAllRead(ME);

    assertEquals(0, service.list(ME).unreadCount());
    assertEquals(1, service.list(OTHER).unreadCount());
    assertTrue(service.list(ME).notifications().stream().allMatch(Notification::read));
  }
}
