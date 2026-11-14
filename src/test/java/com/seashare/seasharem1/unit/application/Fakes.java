package com.seashare.seasharem1.unit.application;

import com.seashare.seasharem1.application.port.out.ImageStoragePort;
import com.seashare.seasharem1.application.port.out.NotificationRepositoryPort;
import com.seashare.seasharem1.application.port.out.PortRepositoryPort;
import com.seashare.seasharem1.application.port.out.StoredImage;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.ExternalServiceUnavailableException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Notification;
import com.seashare.seasharem1.domain.model.NotificationType;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Implementaciones en memoria de los puertos de salida, para probar los casos de uso sin base. */
final class Fakes {

  private Fakes() {}

  static final class Vessels implements VesselRepositoryPort {

    final Map<UUID, Vessel> store = new LinkedHashMap<>();
    final Map<UUID, UUID> deletedLog = new LinkedHashMap<>();

    /** Puertos donde {@link #applyEdit} crea o actualiza el puerto de la edición. */
    Ports ports = new Ports();

    @Override
    public Vessel save(Vessel vessel) {
      boolean taken =
          store.values().stream()
              .anyMatch(
                  other ->
                      !other.id().equals(vessel.id())
                          && !other.deleted()
                          && other.legalRegistration().equals(vessel.legalRegistration()));
      if (taken) {
        throw new BusinessConflictException(
            RegistrationRules.LEGAL_REGISTRATION_TAKEN,
            List.of(
                new FieldViolation(
                    "legal_registration", RegistrationRules.LEGAL_REGISTRATION_TAKEN)));
      }
      store.put(vessel.id(), vessel);
      return vessel;
    }

    @Override
    public Optional<Vessel> findById(UUID id) {
      return Optional.ofNullable(store.get(id)).filter(vessel -> !vessel.deleted());
    }

    @Override
    public Optional<Vessel> findDraftByOwner(UUID ownerId) {
      return store.values().stream()
          .filter(v -> v.ownerId().equals(ownerId) && v.isDraft() && !v.deleted())
          .findFirst();
    }

    @Override
    public List<Vessel> findRegisteredByIds(java.util.Collection<UUID> ids) {
      return store.values().stream()
          .filter(v -> ids.contains(v.id()) && v.status() != VesselStatus.DRAFT && !v.deleted())
          .toList();
    }

    @Override
    public List<Vessel> findAllRegistered() {
      return new ArrayList<>(
          store.values().stream()
              .filter(v -> v.status() != VesselStatus.DRAFT && !v.deleted())
              .toList());
    }

    @Override
    public List<Vessel> findRegisteredByOwner(UUID ownerId) {
      return new ArrayList<>(
          store.values().stream()
              .filter(v -> v.ownerId().equals(ownerId) && v.status() != VesselStatus.DRAFT)
              .filter(v -> !v.deleted())
              .toList());
    }

    @Override
    public boolean existsByLegalRegistration(String legalRegistration) {
      return store.values().stream()
          .anyMatch(v -> !v.deleted() && v.legalRegistration().equals(legalRegistration));
    }

    @Override
    public boolean markDeleted(UUID id, UUID deletedBy, LocalDateTime deletedAt) {
      Vessel vessel = store.get(id);
      if (vessel == null || !vessel.canBeDeleted()) {
        return false;
      }
      store.put(id, vessel.asDeleted());
      deletedLog.put(id, deletedBy);
      return true;
    }

    @Override
    public boolean startMaintenance(UUID id, UUID ownerId, MaintenanceBy by) {
      Vessel vessel = store.get(id);
      if (vessel == null || !vessel.ownerId().equals(ownerId) || !vessel.canBeSentToMaintenance()) {
        return false;
      }
      store.put(id, withState(vessel, VesselStatus.MAINTENANCE, by));
      return true;
    }

    @Override
    public boolean finishMaintenance(UUID id, UUID ownerId) {
      Vessel vessel = store.get(id);
      if (vessel == null
          || !vessel.ownerId().equals(ownerId)
          || !vessel.ownerCanFinishMaintenance()) {
        return false;
      }
      store.put(id, withState(vessel, VesselStatus.AVAILABLE, null));
      return true;
    }

    @Override
    public boolean startMaintenanceAsAdmin(UUID id) {
      Vessel vessel = store.get(id);
      if (vessel == null || !vessel.canBeSentToMaintenance()) {
        return false;
      }
      store.put(id, withState(vessel, VesselStatus.MAINTENANCE, MaintenanceBy.ADMIN));
      return true;
    }

    @Override
    public boolean finishMaintenanceAsAdmin(UUID id) {
      Vessel vessel = store.get(id);
      if (vessel == null || !vessel.adminCanFinishMaintenance()) {
        return false;
      }
      store.put(id, withState(vessel, VesselStatus.AVAILABLE, null));
      return true;
    }

    /** Momentos del último cambio de estado y bitácora de cambios automáticos. */
    final Map<UUID, LocalDateTime> changedAt = new LinkedHashMap<>();

    final List<String> history = new ArrayList<>();

    @Override
    public Optional<LocalDateTime> findStatusChangedAt(UUID id) {
      return Optional.ofNullable(changedAt.get(id));
    }

    @Override
    public boolean changeStatusAutomatically(
        UUID id, VesselStatus expected, VesselStatus next, LocalDateTime eventAt, String eventId) {
      Vessel v = store.get(id);
      if (v == null
          || v.deleted()
          || v.status() != expected
          || (changedAt.containsKey(id) && changedAt.get(id).isAfter(eventAt))) {
        return false;
      }
      store.put(
          id,
          new Vessel(
              v.id(),
              v.name(),
              v.legalRegistration(),
              v.type(),
              v.maxCapacity(),
              v.photoPath(),
              v.hasLifeJackets(),
              v.hasCoolerWithIce(),
              v.hasSoundSystem(),
              v.hasFishingGear(),
              v.hasDivingGear(),
              v.baseRate(),
              next,
              null,
              v.portId(),
              v.ownerId(),
              v.deleted(),
              v.registeredAt()));
      changedAt.put(id, eventAt);
      history.add(expected + ">" + next + ":" + eventId);
      return true;
    }

    @Override
    public boolean markReviewRequested(UUID id, UUID ownerId) {
      Vessel v = store.get(id);
      if (v == null
          || !v.ownerId().equals(ownerId)
          || !v.adminCanFinishMaintenance()
          || v.reviewRequested()) {
        return false;
      }
      store.put(
          id,
          new Vessel(
              v.id(),
              v.name(),
              v.legalRegistration(),
              v.type(),
              v.maxCapacity(),
              v.photoPath(),
              v.hasLifeJackets(),
              v.hasCoolerWithIce(),
              v.hasSoundSystem(),
              v.hasFishingGear(),
              v.hasDivingGear(),
              v.baseRate(),
              v.status(),
              v.maintenanceBy(),
              v.portId(),
              v.ownerId(),
              v.deleted(),
              v.registeredAt(),
              true));
      return true;
    }

    private static Vessel withState(Vessel v, VesselStatus status, MaintenanceBy by) {
      return new Vessel(
          v.id(),
          v.name(),
          v.legalRegistration(),
          v.type(),
          v.maxCapacity(),
          v.photoPath(),
          v.hasLifeJackets(),
          v.hasCoolerWithIce(),
          v.hasSoundSystem(),
          v.hasFishingGear(),
          v.hasDivingGear(),
          v.baseRate(),
          status,
          by,
          v.portId(),
          v.ownerId(),
          v.deleted(),
          v.registeredAt());
    }

    @Override
    public boolean applyEdit(Vessel edited, Port port) {
      Vessel current = store.get(edited.id());
      if (current == null || !current.canBeEdited()) {
        return false;
      }
      Port saved = ports.upsertByName(port);
      store.put(edited.id(), withPort(edited, saved.id()));
      return true;
    }

    private static Vessel withPort(Vessel vessel, UUID portId) {
      return vessel.edited(
          vessel.name(),
          vessel.maxCapacity(),
          vessel.photoPath(),
          vessel.additionalServices(),
          vessel.baseRate(),
          portId);
    }

    @Override
    public void deleteById(UUID id) {
      store.remove(id);
    }
  }

  static final class Notifications implements NotificationRepositoryPort {

    final List<Notification> store = new ArrayList<>();
    boolean failing;

    @Override
    public Notification save(Notification notification) {
      if (failing) {
        throw new IllegalStateException("fallo simulado");
      }
      store.add(notification);
      return notification;
    }

    @Override
    public List<Notification> findLatestByRecipient(UUID recipientId, int limit) {
      return store.stream()
          .filter(n -> n.recipientId().equals(recipientId))
          .sorted(java.util.Comparator.comparing(Notification::createdAt).reversed())
          .limit(limit)
          .toList();
    }

    @Override
    public boolean existsUnread(UUID recipientId, UUID vesselId, NotificationType type) {
      return store.stream()
          .anyMatch(
              n ->
                  n.recipientId().equals(recipientId)
                      && n.vesselId().equals(vesselId)
                      && n.type() == type
                      && !n.read());
    }

    @Override
    public long countUnread(UUID recipientId) {
      return store.stream().filter(n -> n.recipientId().equals(recipientId) && !n.read()).count();
    }

    @Override
    public boolean markRead(UUID notificationId, UUID recipientId) {
      for (int i = 0; i < store.size(); i++) {
        Notification n = store.get(i);
        if (n.id().equals(notificationId) && n.recipientId().equals(recipientId)) {
          store.set(
              i,
              new Notification(
                  n.id(),
                  n.recipientId(),
                  n.vesselId(),
                  n.type(),
                  n.message(),
                  true,
                  n.createdAt()));
          return true;
        }
      }
      return false;
    }

    @Override
    public void markAllRead(UUID recipientId) {
      store.stream()
          .filter(n -> n.recipientId().equals(recipientId))
          .map(Notification::id)
          .toList()
          .forEach(id -> markRead(id, recipientId));
    }
  }

  static final class Ports implements PortRepositoryPort {

    final List<Port> store = new ArrayList<>();

    @Override
    public Optional<Port> findByName(String name) {
      return store.stream().filter(port -> port.name().equalsIgnoreCase(name)).findFirst();
    }

    @Override
    public Optional<Port> findById(UUID id) {
      return store.stream().filter(port -> port.id().equals(id)).findFirst();
    }

    @Override
    public Port save(Port port) {
      store.add(port);
      return port;
    }

    /** Como el adaptador real: si el nombre existe actualiza sus coordenadas; si no, lo crea. */
    Port upsertByName(Port port) {
      Optional<Port> existing = findByName(port.name());
      if (existing.isEmpty()) {
        store.add(port);
        return port;
      }
      Port updated =
          new Port(existing.get().id(), existing.get().name(), port.latitude(), port.longitude());
      store.set(store.indexOf(existing.get()), updated);
      return updated;
    }
  }

  static final class Images implements ImageStoragePort {

    int calls;
    String lastContentType;
    String lastFolder;
    boolean failing;

    @Override
    public StoredImage store(byte[] content, String contentType, String folder) {
      calls++;
      lastContentType = contentType;
      lastFolder = folder;
      if (failing) {
        throw new ExternalServiceUnavailableException("Cloudinary caído", null);
      }
      return new StoredImage(
          "https://res.cloudinary.com/test/image/upload/a.png", "seashare/boats/a");
    }
  }

  /** Avisos procesados en memoria. */
  static final class ProcessedEvents
      implements com.seashare.seasharem1.application.port.out.ProcessedEventRepositoryPort {
    final java.util.Set<String> ids = new java.util.HashSet<>();

    @Override
    public boolean exists(String eventId) {
      return ids.contains(eventId);
    }

    @Override
    public boolean register(String eventId, UUID vesselId, LocalDateTime processedAt) {
      return ids.add(eventId);
    }
  }
}
