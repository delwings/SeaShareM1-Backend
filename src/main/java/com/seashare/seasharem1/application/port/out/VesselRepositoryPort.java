package com.seashare.seasharem1.application.port.out;

import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida para guardar y consultar embarcaciones. */
public interface VesselRepositoryPort {

  /**
   * Inserta o actualiza.
   *
   * @throws com.seashare.seasharem1.domain.exception.BusinessConflictException si otra embarcación
   *     ya tiene esa matrícula (también cuando dos guardados llegan al mismo tiempo)
   */
  Vessel save(Vessel vessel);

  /** Embarcación no eliminada con ese id. */
  Optional<Vessel> findById(UUID id);

  /** Embarcaciones registradas (ni borrador ni eliminadas) entre los identificadores dados. */
  List<Vessel> findRegisteredByIds(java.util.Collection<UUID> ids);

  /**
   * Todas las embarcaciones registradas (no borradores ni eliminadas), de cualquier propietario.
   */
  List<Vessel> findAllRegistered();

  /** Borrador del propietario (a lo sumo uno). */
  Optional<Vessel> findDraftByOwner(UUID ownerId);

  /** Embarcaciones del propietario que no son borrador ni están eliminadas, en cualquier orden. */
  List<Vessel> findRegisteredByOwner(UUID ownerId);

  /** Indica si alguna embarcación no eliminada (incluidos borradores) usa esa matrícula. */
  boolean existsByLegalRegistration(String legalRegistration);

  /**
   * Eliminación lógica atómica: marca la embarcación como eliminada, con fecha y autor, solo si
   * sigue sin eliminar y en un estado que lo permite.
   *
   * @return {@code true} si la marcó; {@code false} si otro cambio se adelantó (cambió de estado o
   *     ya estaba eliminada)
   */
  boolean markDeleted(UUID id, UUID deletedBy, LocalDateTime deletedAt);

  /**
   * Pasa de Disponible a En Mantenimiento/Limpieza de forma atómica, anotando quién la envió, solo
   * si sigue sin eliminar, es del propietario y continúa Disponible.
   *
   * @return {@code true} si cambió; {@code false} si otro cambio se adelantó
   */
  boolean startMaintenance(UUID id, UUID ownerId, MaintenanceBy by);

  /**
   * Pasa de En Mantenimiento/Limpieza a Disponible de forma atómica, solo si sigue sin eliminar, es
   * del propietario, continúa en mantenimiento y no la suspendió el administrador.
   *
   * @return {@code true} si cambió; {@code false} si otro cambio se adelantó
   */
  boolean finishMaintenance(UUID id, UUID ownerId);

  /**
   * Pasa de Disponible a En Mantenimiento/Limpieza por decisión del administrador (cualquier
   * propietario), anotando origen ADMIN, solo si sigue sin eliminar y Disponible.
   */
  boolean startMaintenanceAsAdmin(UUID id);

  /**
   * Pasa de En Mantenimiento/Limpieza a Disponible por decisión del administrador, solo si sigue
   * sin eliminar y fue él quien la envió (origen ADMIN).
   */
  boolean finishMaintenanceAsAdmin(UUID id);

  /**
   * Marca que el propietario solicitó la revisión, de forma atómica: solo si la embarcación es
   * suya, sigue sin eliminar, está en mantenimiento por el administrador y aún no la había
   * solicitado. Devuelve falso si no cambió nada (p. ej. ya estaba solicitada).
   */
  boolean markReviewRequested(UUID id, UUID ownerId);

  /** Momento (UTC) del último cambio de estado, si alguno quedó registrado. */
  Optional<LocalDateTime> findStatusChangedAt(UUID id);

  /**
   * Cambio de estado por un aviso del Módulo 2, de forma atómica: solo se aplica si la embarcación
   * sigue sin eliminar, en el estado {@code expected} y sin un cambio posterior a {@code eventAt}.
   * Deja la embarcación sin origen de mantenimiento, guarda la fecha del cambio y anota el
   * historial como automático. Devuelve falso si no cambió nada.
   */
  boolean changeStatusAutomatically(
      UUID id, VesselStatus expected, VesselStatus next, LocalDateTime eventAt, String eventId);

  /**
   * Guarda una edición de forma atómica: crea el puerto si no existe o actualiza sus coordenadas si
   * ya existe con ese nombre (los puertos son compartidos), y actualiza los datos editables de la
   * embarcación, pero solo si sigue sin eliminar y en un estado editable. Si no, no cambia nada.
   *
   * @param edited embarcación con los datos nuevos; su portId se reemplaza por el del puerto
   *     resultante
   * @param port puerto deseado (nombre y coordenadas)
   * @return {@code true} si se guardó; {@code false} si otro cambio se adelantó (cambió de estado o
   *     fue eliminada)
   */
  boolean applyEdit(Vessel edited, Port port);

  /** Eliminación física (solo borradores). */
  void deleteById(UUID id);
}
