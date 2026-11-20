package com.seashare.seasharem1.infrastructure.adapter.out.persistence.repository;

import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity.VesselEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/** Repositorio Spring Data de embarcaciones. Solo lo usa el adaptador de persistencia. */
public interface VesselJpaRepository extends JpaRepository<VesselEntity, UUID> {

  Optional<VesselEntity> findByOwnerIdAndStatusAndDeletedFalse(UUID ownerId, VesselStatus status);

  List<VesselEntity> findByOwnerIdAndStatusNotAndDeletedFalse(UUID ownerId, VesselStatus status);

  List<VesselEntity> findByStatusNotAndDeletedFalse(VesselStatus status);

  List<VesselEntity> findByIdInAndStatusNotAndDeletedFalse(
      Collection<UUID> ids, VesselStatus status);

  boolean existsByLegalRegistrationAndDeletedFalse(String legalRegistration);

  /** Catálogo paginado: registradas, por nombre; el filtro mira el nombre y el puerto. */
  @Query(
      value =
          "SELECT v.* FROM vessel v LEFT JOIN port p ON p.id = v.port_id "
              + "WHERE v.deleted = FALSE AND v.status <> 'DRAFT' "
              + "AND (:q IS NULL OR LOWER(v.name) LIKE :q OR LOWER(p.name) LIKE :q) "
              + "ORDER BY v.name, v.id",
      countQuery =
          "SELECT COUNT(*) FROM vessel v LEFT JOIN port p ON p.id = v.port_id "
              + "WHERE v.deleted = FALSE AND v.status <> 'DRAFT' "
              + "AND (:q IS NULL OR LOWER(v.name) LIKE :q OR LOWER(p.name) LIKE :q)",
      nativeQuery = true)
  org.springframework.data.domain.Page<VesselEntity> searchRegistered(
      @Param("q") String likePattern, org.springframework.data.domain.Pageable pageable);

  /**
   * Eliminación lógica en una sola sentencia: solo actúa si la embarcación sigue sin eliminar y en
   * uno de los estados dados. Devuelve las filas afectadas (0 o 1). Se escribe nativa porque la
   * fecha y el autor de la eliminación no están en la entidad.
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET deleted = TRUE, deleted_at = :deletedAt, deleted_by = :deletedBy "
              + "WHERE id = :id AND deleted = FALSE AND status IN (:statuses)",
      nativeQuery = true)
  int markDeleted(
      @Param("id") String id,
      @Param("deletedBy") String deletedBy,
      @Param("deletedAt") LocalDateTime deletedAt,
      @Param("statuses") Collection<String> statuses);

  /** Pasa de Disponible a mantenimiento en una sola sentencia; devuelve las filas afectadas. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET status = 'MAINTENANCE', maintenance_by = :by, review_requested ="
              + " FALSE, maintenance_start = :start, maintenance_until = :until, status_changed_at"
              + " = :at WHERE id = :id AND owner_id = :ownerId AND deleted = FALSE AND status ="
              + " 'AVAILABLE'",
      nativeQuery = true)
  int startMaintenance(
      @Param("id") String id,
      @Param("ownerId") String ownerId,
      @Param("by") String by,
      @Param("start") LocalDateTime start,
      @Param("until") LocalDateTime until,
      @Param("at") LocalDateTime at);

  /**
   * Pasa de mantenimiento a Disponible en una sola sentencia, salvo suspensión del administrador;
   * devuelve las filas afectadas.
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET status = 'AVAILABLE', maintenance_by = NULL, review_requested = FALSE,"
              + " maintenance_start = NULL, maintenance_until = NULL, status_changed_at = :at WHERE"
              + " id = :id AND owner_id = :ownerId AND deleted = FALSE AND status = 'MAINTENANCE'"
              + " AND (maintenance_by IS NULL OR maintenance_by <> 'ADMIN')",
      nativeQuery = true)
  int finishMaintenance(
      @Param("id") String id, @Param("ownerId") String ownerId, @Param("at") LocalDateTime at);

  /** Disponible a mantenimiento por el administrador, de cualquier propietario. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET status = 'MAINTENANCE', maintenance_by = 'ADMIN', review_requested ="
              + " FALSE, maintenance_start = :start, maintenance_until = :until, status_changed_at"
              + " = :at WHERE id = :id AND deleted = FALSE AND status = 'AVAILABLE'",
      nativeQuery = true)
  int startMaintenanceAsAdmin(
      @Param("id") String id,
      @Param("start") LocalDateTime start,
      @Param("until") LocalDateTime until,
      @Param("at") LocalDateTime at);

  /** Mantenimiento a Disponible por el administrador, solo si él la envió. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET status = 'AVAILABLE', maintenance_by = NULL, review_requested = FALSE,"
              + " maintenance_start = NULL, maintenance_until = NULL, status_changed_at = :at WHERE"
              + " id = :id AND deleted = FALSE AND status = 'MAINTENANCE' AND maintenance_by ="
              + " 'ADMIN'",
      nativeQuery = true)
  int finishMaintenanceAsAdmin(@Param("id") String id, @Param("at") LocalDateTime at);

  /**
   * Cambio de estado por un aviso del Módulo 2: solo si sigue en el estado esperado, sin eliminar y
   * sin un cambio posterior al del aviso. Deja el mantenimiento sin origen (lo termina el dueño).
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET status = :next, maintenance_by = NULL, review_requested = FALSE, "
              + "maintenance_start = NULL, maintenance_until = NULL, status_changed_at = :at "
              + "WHERE id = :id AND deleted = FALSE AND status = :expected "
              + "AND (status_changed_at IS NULL OR status_changed_at <= :at)",
      nativeQuery = true)
  int changeStatus(
      @Param("id") String id,
      @Param("expected") String expected,
      @Param("next") String next,
      @Param("at") LocalDateTime at);

  /** Marca la revisión como solicitada una sola vez por mantenimiento del administrador. */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET review_requested = TRUE "
              + "WHERE id = :id AND owner_id = :ownerId AND deleted = FALSE "
              + "AND status = 'MAINTENANCE' AND maintenance_by = 'ADMIN' "
              + "AND review_requested = FALSE",
      nativeQuery = true)
  int markReviewRequested(@Param("id") String id, @Param("ownerId") String ownerId);

  /**
   * Actualiza los datos editables en una sola sentencia: solo actúa si la embarcación sigue sin
   * eliminar, es del propietario y está en uno de los estados dados. Devuelve las filas afectadas
   * (0 o 1).
   */
  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE vessel SET name = :name, max_capacity = :capacity, photo_path = :photoPath, "
              + "base_rate = :baseRate, port_id = :portId, has_life_jackets = :lifeJackets, "
              + "has_cooler_with_ice = :coolerWithIce, has_sound_system = :soundSystem, "
              + "has_fishing_gear = :fishingGear, has_diving_gear = :divingGear "
              + "WHERE id = :id AND owner_id = :ownerId AND deleted = FALSE "
              + "AND status IN (:statuses)",
      nativeQuery = true)
  int updateEditable(
      @Param("id") String id,
      @Param("ownerId") String ownerId,
      @Param("name") String name,
      @Param("capacity") int capacity,
      @Param("photoPath") String photoPath,
      @Param("baseRate") BigDecimal baseRate,
      @Param("portId") String portId,
      @Param("lifeJackets") boolean lifeJackets,
      @Param("coolerWithIce") boolean coolerWithIce,
      @Param("soundSystem") boolean soundSystem,
      @Param("fishingGear") boolean fishingGear,
      @Param("divingGear") boolean divingGear,
      @Param("statuses") Collection<String> statuses);
}
