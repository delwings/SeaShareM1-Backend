package com.seashare.seasharem1.domain.model;

import com.seashare.seasharem1.domain.exception.BusinessConflictException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Embarcación. Mientras el estado es DRAFT solo están completos los datos del paso 1 del registro
 * (name, legalRegistration, type, maxCapacity); los del paso 2 pueden ser nulos.
 *
 * @param photoPath URL de la foto en Cloudinary
 * @param baseRate tarifa base por hora en COP
 * @param maintenanceBy solo tiene valor cuando la embarcación está en mantenimiento
 * @param portId puerto de atraque, nulo hasta completar el paso 2
 * @param deleted borrado lógico
 * @param registeredAt momento (UTC) en que pasó a AVAILABLE por primera vez; nulo en el borrador
 * @param reviewRequested el propietario ya solicitó la revisión del mantenimiento puesto por el
 *     administrador (solo se puede pedir una vez por mantenimiento)
 */
public record Vessel(
    UUID id,
    String name,
    String legalRegistration,
    VesselType type,
    int maxCapacity,
    String photoPath,
    Boolean hasLifeJackets,
    Boolean hasCoolerWithIce,
    Boolean hasSoundSystem,
    Boolean hasFishingGear,
    Boolean hasDivingGear,
    BigDecimal baseRate,
    VesselStatus status,
    MaintenanceBy maintenanceBy,
    UUID portId,
    UUID ownerId,
    boolean deleted,
    LocalDateTime registeredAt,
    boolean reviewRequested) {

  /** Constructor sin la marca de revisión solicitada (queda en falso). */
  public Vessel(
      UUID id,
      String name,
      String legalRegistration,
      VesselType type,
      int maxCapacity,
      String photoPath,
      Boolean hasLifeJackets,
      Boolean hasCoolerWithIce,
      Boolean hasSoundSystem,
      Boolean hasFishingGear,
      Boolean hasDivingGear,
      BigDecimal baseRate,
      VesselStatus status,
      MaintenanceBy maintenanceBy,
      UUID portId,
      UUID ownerId,
      boolean deleted,
      LocalDateTime registeredAt) {
    this(
        id,
        name,
        legalRegistration,
        type,
        maxCapacity,
        photoPath,
        hasLifeJackets,
        hasCoolerWithIce,
        hasSoundSystem,
        hasFishingGear,
        hasDivingGear,
        baseRate,
        status,
        maintenanceBy,
        portId,
        ownerId,
        deleted,
        registeredAt,
        false);
  }

  /** Crea el borrador que resulta de completar el paso 1 del registro. */
  public static Vessel newDraft(
      UUID id,
      UUID ownerId,
      String name,
      String legalRegistration,
      VesselType type,
      int maxCapacity) {
    return new Vessel(
        id,
        name,
        legalRegistration,
        type,
        maxCapacity,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        VesselStatus.DRAFT,
        null,
        null,
        ownerId,
        false,
        null,
        false);
  }

  /** Estados en los que se puede eliminar: Disponible y En Mantenimiento/Limpieza. */
  public static final Set<VesselStatus> DELETABLE_STATUSES =
      Set.of(VesselStatus.AVAILABLE, VesselStatus.MAINTENANCE);

  /** Indica si el estado actual permite eliminar la embarcación. */
  public boolean canBeDeleted() {
    return !deleted && DELETABLE_STATUSES.contains(status);
  }

  /** Estados en los que se puede editar: Disponible y En Mantenimiento/Limpieza. */
  public static final Set<VesselStatus> EDITABLE_STATUSES =
      Set.of(VesselStatus.AVAILABLE, VesselStatus.MAINTENANCE);

  /** Indica si el estado actual permite editar la embarcación. */
  public boolean canBeEdited() {
    return !deleted && EDITABLE_STATUSES.contains(status);
  }

  /**
   * Copia con los datos editables cambiados. La matrícula, el tipo, el estado, el propietario y la
   * fecha de registro no cambian nunca.
   */
  public Vessel edited(
      String newName,
      int newCapacity,
      String newPhotoPath,
      AdditionalServices services,
      BigDecimal newBaseRate,
      UUID newPortId) {
    return new Vessel(
        id,
        newName,
        legalRegistration,
        type,
        newCapacity,
        newPhotoPath,
        services.lifeJackets(),
        services.coolerWithIce(),
        services.soundSystem(),
        services.fishingGear(),
        services.divingGear(),
        newBaseRate,
        status,
        maintenanceBy,
        newPortId,
        ownerId,
        deleted,
        registeredAt,
        reviewRequested);
  }

  /** Indica si el propietario puede enviarla a mantenimiento: solo si está Disponible. */
  public boolean canBeSentToMaintenance() {
    return !deleted && status == VesselStatus.AVAILABLE;
  }

  /**
   * Indica si el propietario puede terminar el mantenimiento: está en mantenimiento y no la
   * suspendió el administrador (en ese caso solo el administrador la habilita).
   */
  public boolean ownerCanFinishMaintenance() {
    return !deleted && status == VesselStatus.MAINTENANCE && maintenanceBy != MaintenanceBy.ADMIN;
  }

  /**
   * Indica si el administrador puede terminar el mantenimiento: solo si él mismo la envió. Si la
   * envió el propietario (o el sistema), solo ese origen puede terminarla.
   */
  public boolean adminCanFinishMaintenance() {
    return !deleted && status == VesselStatus.MAINTENANCE && maintenanceBy == MaintenanceBy.ADMIN;
  }

  /** Copia marcada como eliminada (la fecha y el autor se guardan solo en la base de datos). */
  public Vessel asDeleted() {
    return new Vessel(
        id,
        name,
        legalRegistration,
        type,
        maxCapacity,
        photoPath,
        hasLifeJackets,
        hasCoolerWithIce,
        hasSoundSystem,
        hasFishingGear,
        hasDivingGear,
        baseRate,
        status,
        maintenanceBy,
        portId,
        ownerId,
        true,
        registeredAt,
        reviewRequested);
  }

  public boolean isDraft() {
    return status == VesselStatus.DRAFT;
  }

  /**
   * Completa el registro (paso 2): pasa de DRAFT a AVAILABLE con los datos complementarios y la
   * fecha en que quedó registrada.
   *
   * @throws BusinessConflictException si la embarcación no es un borrador
   */
  public Vessel complete(
      String photoPath,
      AdditionalServices services,
      BigDecimal baseRate,
      UUID portId,
      LocalDateTime registeredAt) {
    if (!isDraft()) {
      throw new BusinessConflictException("La embarcación no está en estado de borrador.");
    }
    return new Vessel(
        id,
        name,
        legalRegistration,
        type,
        maxCapacity,
        photoPath,
        services.lifeJackets(),
        services.coolerWithIce(),
        services.soundSystem(),
        services.fishingGear(),
        services.divingGear(),
        baseRate,
        VesselStatus.AVAILABLE,
        null,
        portId,
        ownerId,
        deleted,
        registeredAt,
        false);
  }

  /** Servicios adicionales de la embarcación; los valores nulos del borrador cuentan como no. */
  public AdditionalServices additionalServices() {
    return new AdditionalServices(
        Boolean.TRUE.equals(hasLifeJackets),
        Boolean.TRUE.equals(hasCoolerWithIce),
        Boolean.TRUE.equals(hasSoundSystem),
        Boolean.TRUE.equals(hasFishingGear),
        Boolean.TRUE.equals(hasDivingGear));
  }
}
