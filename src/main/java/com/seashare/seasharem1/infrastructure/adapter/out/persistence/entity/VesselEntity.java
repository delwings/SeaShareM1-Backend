package com.seashare.seasharem1.infrastructure.adapter.out.persistence.entity;

import com.seashare.seasharem1.domain.model.MaintenanceBy;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA de la tabla vessel. Las claves foráneas (port_id, owner_id) se guardan como UUID
 * simples: la integridad la garantiza la base de datos y las consultas con unión se escriben en
 * JPQL cuando hagan falta.
 */
@Entity
@Table(name = "vessel")
public class VesselEntity {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", length = 36, nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", length = 100, nullable = false)
  private String name;

  @Column(name = "legal_registration", length = 30, nullable = false)
  private String legalRegistration;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private VesselType type;

  @Column(name = "max_capacity", nullable = false)
  private short maxCapacity;

  @Column(name = "photo_path")
  private String photoPath;

  @Column(name = "has_life_jackets")
  private Boolean hasLifeJackets;

  @Column(name = "has_cooler_with_ice")
  private Boolean hasCoolerWithIce;

  @Column(name = "has_sound_system")
  private Boolean hasSoundSystem;

  @Column(name = "has_fishing_gear")
  private Boolean hasFishingGear;

  @Column(name = "has_diving_gear")
  private Boolean hasDivingGear;

  @Column(name = "base_rate", precision = 12, scale = 2)
  private BigDecimal baseRate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private VesselStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "maintenance_by")
  private MaintenanceBy maintenanceBy;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "port_id", length = 36)
  private UUID portId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "owner_id", length = 36, nullable = false)
  private UUID ownerId;

  @Column(name = "deleted", nullable = false)
  private boolean deleted;

  @Column(name = "registered_at")
  private LocalDateTime registeredAt;

  @Column(name = "review_requested", nullable = false)
  private boolean reviewRequested;

  public boolean isReviewRequested() {
    return reviewRequested;
  }

  public void setReviewRequested(boolean reviewRequested) {
    this.reviewRequested = reviewRequested;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLegalRegistration() {
    return legalRegistration;
  }

  public void setLegalRegistration(String legalRegistration) {
    this.legalRegistration = legalRegistration;
  }

  public VesselType getType() {
    return type;
  }

  public void setType(VesselType type) {
    this.type = type;
  }

  public short getMaxCapacity() {
    return maxCapacity;
  }

  public void setMaxCapacity(short maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  public String getPhotoPath() {
    return photoPath;
  }

  public void setPhotoPath(String photoPath) {
    this.photoPath = photoPath;
  }

  public Boolean getHasLifeJackets() {
    return hasLifeJackets;
  }

  public void setHasLifeJackets(Boolean hasLifeJackets) {
    this.hasLifeJackets = hasLifeJackets;
  }

  public Boolean getHasCoolerWithIce() {
    return hasCoolerWithIce;
  }

  public void setHasCoolerWithIce(Boolean hasCoolerWithIce) {
    this.hasCoolerWithIce = hasCoolerWithIce;
  }

  public Boolean getHasSoundSystem() {
    return hasSoundSystem;
  }

  public void setHasSoundSystem(Boolean hasSoundSystem) {
    this.hasSoundSystem = hasSoundSystem;
  }

  public Boolean getHasFishingGear() {
    return hasFishingGear;
  }

  public void setHasFishingGear(Boolean hasFishingGear) {
    this.hasFishingGear = hasFishingGear;
  }

  public Boolean getHasDivingGear() {
    return hasDivingGear;
  }

  public void setHasDivingGear(Boolean hasDivingGear) {
    this.hasDivingGear = hasDivingGear;
  }

  public BigDecimal getBaseRate() {
    return baseRate;
  }

  public void setBaseRate(BigDecimal baseRate) {
    this.baseRate = baseRate;
  }

  public VesselStatus getStatus() {
    return status;
  }

  public void setStatus(VesselStatus status) {
    this.status = status;
  }

  public MaintenanceBy getMaintenanceBy() {
    return maintenanceBy;
  }

  public void setMaintenanceBy(MaintenanceBy maintenanceBy) {
    this.maintenanceBy = maintenanceBy;
  }

  public UUID getPortId() {
    return portId;
  }

  public void setPortId(UUID portId) {
    this.portId = portId;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  public LocalDateTime getRegisteredAt() {
    return registeredAt;
  }

  public void setRegisteredAt(LocalDateTime registeredAt) {
    this.registeredAt = registeredAt;
  }
}
