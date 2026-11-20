package com.seashare.seasharem1.infrastructure.adapter.out.persistence;

import com.seashare.seasharem1.application.port.out.VesselPage;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.MaintenanceWindow;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.rules.RegistrationRules;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.PortEntity;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.mapper.VesselPersistenceMapper;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository.PortJpaRepository;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository.VesselJpaRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/** Adaptador que implementa el puerto de embarcaciones con Spring Data JPA. */
@Component
public class VesselPersistenceAdapter implements VesselRepositoryPort {

  private final VesselJpaRepository repository;
  private final VesselPersistenceMapper mapper;
  private final PortJpaRepository portRepository;
  private final TransactionTemplate transaction;
  private final JdbcTemplate jdbc;

  public VesselPersistenceAdapter(
      VesselJpaRepository repository,
      VesselPersistenceMapper mapper,
      PortJpaRepository portRepository,
      PlatformTransactionManager transactionManager,
      JdbcTemplate jdbc) {
    this.repository = repository;
    this.mapper = mapper;
    this.portRepository = portRepository;
    this.transaction = new TransactionTemplate(transactionManager);
    this.jdbc = jdbc;
  }

  @Override
  public Vessel save(Vessel vessel) {
    try {
      return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(vessel)));
    } catch (DataIntegrityViolationException exception) {
      // Dos guardados con la misma matrícula al mismo tiempo: gana el primero, la base rechaza el
      // otro.
      Throwable cause = exception.getMostSpecificCause();
      String detail = cause.getMessage() == null ? "" : cause.getMessage();
      if (detail.contains("uq_vessel_active_legal_registration")) {
        throw new BusinessConflictException(
            RegistrationRules.LEGAL_REGISTRATION_TAKEN,
            List.of(
                new FieldViolation(
                    "legal_registration", RegistrationRules.LEGAL_REGISTRATION_TAKEN)));
      }
      throw exception;
    }
  }

  @Override
  public Optional<Vessel> findById(UUID id) {
    return repository.findById(id).filter(entity -> !entity.isDeleted()).map(mapper::toDomain);
  }

  @Override
  public Optional<Vessel> findDraftByOwner(UUID ownerId) {
    return repository
        .findByOwnerIdAndStatusAndDeletedFalse(ownerId, VesselStatus.DRAFT)
        .map(mapper::toDomain);
  }

  @Override
  public List<Vessel> findRegisteredByOwner(UUID ownerId) {
    return repository.findByOwnerIdAndStatusNotAndDeletedFalse(ownerId, VesselStatus.DRAFT).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Vessel> findRegisteredByIds(java.util.Collection<UUID> ids) {
    return repository.findByIdInAndStatusNotAndDeletedFalse(ids, VesselStatus.DRAFT).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Vessel> findAllRegistered() {
    return repository.findByStatusNotAndDeletedFalse(VesselStatus.DRAFT).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public boolean existsByLegalRegistration(String legalRegistration) {
    return repository.existsByLegalRegistrationAndDeletedFalse(legalRegistration);
  }

  @Override
  public boolean markDeleted(UUID id, UUID deletedBy, LocalDateTime deletedAt) {
    List<String> statuses = Vessel.DELETABLE_STATUSES.stream().map(Enum::name).toList();
    return repository.markDeleted(id.toString(), deletedBy.toString(), deletedAt, statuses) > 0;
  }

  @Override
  public boolean startMaintenance(
      UUID id, UUID ownerId, MaintenanceBy by, MaintenanceWindow window) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return startWithOutbox(
        id,
        by,
        window,
        now,
        () ->
            repository.startMaintenance(
                id.toString(), ownerId.toString(), by.name(), window.start(), window.until(), now));
  }

  @Override
  public boolean startMaintenanceAsAdmin(UUID id, MaintenanceWindow window) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return startWithOutbox(
        id,
        MaintenanceBy.ADMIN,
        window,
        now,
        () ->
            repository.startMaintenanceAsAdmin(id.toString(), window.start(), window.until(), now));
  }

  /**
   * Inicia el mantenimiento, anota el historial y deja el aviso para el Módulo 2 en una sola
   * transacción: si algo falla, no queda ni el cambio ni el aviso a medias.
   */
  private boolean startWithOutbox(
      UUID id,
      MaintenanceBy by,
      MaintenanceWindow window,
      LocalDateTime now,
      java.util.function.IntSupplier update) {
    Boolean started =
        transaction.execute(
            status -> {
              if (update.getAsInt() == 0) {
                return false;
              }
              insertHistory(
                  id, VesselStatus.AVAILABLE, VesselStatus.MAINTENANCE, now, "MANUAL", null);
              jdbc.update(
                  "INSERT INTO maintenance_event_outbox "
                      + "(event_id, vessel_id, maintenance_start, maintenance_until, occurred_at, "
                      + "reason, status, attempts, next_attempt_at) "
                      + "VALUES (?, ?, ?, ?, ?, ?, 'PENDING', 0, ?)",
                  UUID.randomUUID().toString(),
                  id.toString(),
                  window.start(),
                  window.until(),
                  now,
                  by.name(),
                  now);
              return true;
            });
    return Boolean.TRUE.equals(started);
  }

  @Override
  public Optional<MaintenanceWindow> findMaintenanceWindow(UUID id) {
    return Optional.ofNullable(
        jdbc.query(
            "SELECT maintenance_start, maintenance_until FROM vessel WHERE id = ?",
            rs -> {
              if (!rs.next()) {
                return null;
              }
              LocalDateTime start = rs.getObject(1, LocalDateTime.class);
              LocalDateTime until = rs.getObject(2, LocalDateTime.class);
              return start == null || until == null ? null : new MaintenanceWindow(start, until);
            },
            id.toString()));
  }

  @Override
  public VesselPage searchRegistered(String query, int page, int size) {
    String pattern = null;
    if (query != null && !query.isBlank()) {
      String escaped =
          query.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
      pattern = "%" + escaped + "%";
    }
    var result =
        repository.searchRegistered(
            pattern, org.springframework.data.domain.PageRequest.of(page - 1, size));
    return new VesselPage(
        result.getContent().stream().map(mapper::toDomain).toList(), result.getTotalElements());
  }

  @Override
  public boolean finishMaintenanceAsAdmin(UUID id) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return manualChange(
        id,
        VesselStatus.MAINTENANCE,
        VesselStatus.AVAILABLE,
        now,
        repository.finishMaintenanceAsAdmin(id.toString(), now));
  }

  @Override
  public boolean markReviewRequested(UUID id, UUID ownerId) {
    return repository.markReviewRequested(id.toString(), ownerId.toString()) > 0;
  }

  @Override
  public boolean finishMaintenance(UUID id, UUID ownerId) {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
    return manualChange(
        id,
        VesselStatus.MAINTENANCE,
        VesselStatus.AVAILABLE,
        now,
        repository.finishMaintenance(id.toString(), ownerId.toString(), now));
  }

  @Override
  public Optional<LocalDateTime> findStatusChangedAt(UUID id) {
    return Optional.ofNullable(
        jdbc.query(
            "SELECT status_changed_at FROM vessel WHERE id = ?",
            rs -> rs.next() ? rs.getObject(1, LocalDateTime.class) : null,
            id.toString()));
  }

  @Override
  public boolean changeStatusAutomatically(
      UUID id, VesselStatus expected, VesselStatus next, LocalDateTime eventAt, String eventId) {
    Boolean changed =
        transaction.execute(
            status -> {
              int rows =
                  repository.changeStatus(id.toString(), expected.name(), next.name(), eventAt);
              if (rows == 0) {
                return false;
              }
              insertHistory(id, expected, next, eventAt, "AUTOMATIC", eventId);
              return true;
            });
    return Boolean.TRUE.equals(changed);
  }

  /** Anota en el historial un cambio manual ya aplicado (filas afectadas &gt; 0). */
  private boolean manualChange(
      UUID id, VesselStatus from, VesselStatus to, LocalDateTime at, int rows) {
    if (rows == 0) {
      return false;
    }
    insertHistory(id, from, to, at, "MANUAL", null);
    return true;
  }

  private void insertHistory(
      UUID id,
      VesselStatus from,
      VesselStatus to,
      LocalDateTime at,
      String source,
      String eventId) {
    jdbc.update(
        "INSERT INTO vessel_status_history "
            + "(id, vessel_id, previous_status, new_status, changed_at, source, event_id) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
        UUID.randomUUID().toString(),
        id.toString(),
        from.name(),
        to.name(),
        at,
        source,
        eventId);
  }

  @Override
  public boolean applyEdit(Vessel edited, Port port) {
    try {
      return runEdit(edited, port);
    } catch (DataIntegrityViolationException exception) {
      // Otro guardado creó ese puerto justo ahora: se reintenta una vez y ya lo encuentra.
      return runEdit(edited, port);
    }
  }

  private boolean runEdit(Vessel edited, Port port) {
    return Boolean.TRUE.equals(
        transaction.execute(status -> applyEditInTransaction(edited, port, status)));
  }

  private boolean applyEditInTransaction(Vessel edited, Port port, TransactionStatus status) {
    PortEntity target =
        portRepository
            .findByName(port.name())
            .map(
                existing -> {
                  // Los puertos son compartidos: si el nombre ya existe, se actualizan sus
                  // coordenadas.
                  existing.setLatitude(port.latitude());
                  existing.setLongitude(port.longitude());
                  return portRepository.saveAndFlush(existing);
                })
            .orElseGet(
                () -> {
                  PortEntity created = new PortEntity();
                  created.setId(port.id());
                  created.setName(port.name());
                  created.setLatitude(port.latitude());
                  created.setLongitude(port.longitude());
                  return portRepository.saveAndFlush(created);
                });
    List<String> statuses = Vessel.EDITABLE_STATUSES.stream().map(Enum::name).toList();
    int updated =
        repository.updateEditable(
            edited.id().toString(),
            edited.ownerId().toString(),
            edited.name(),
            edited.maxCapacity(),
            edited.photoPath(),
            edited.baseRate(),
            target.getId().toString(),
            Boolean.TRUE.equals(edited.hasLifeJackets()),
            Boolean.TRUE.equals(edited.hasCoolerWithIce()),
            Boolean.TRUE.equals(edited.hasSoundSystem()),
            Boolean.TRUE.equals(edited.hasFishingGear()),
            Boolean.TRUE.equals(edited.hasDivingGear()),
            statuses);
    if (updated == 0) {
      // El estado cambió o la eliminaron: se deshace también lo hecho en el puerto.
      status.setRollbackOnly();
      return false;
    }
    return true;
  }

  @Override
  public void deleteById(UUID id) {
    repository.deleteById(id);
  }
}
