package com.seashare.seasharem1.domain.model;

import java.time.LocalDateTime;

/**
 * Ventana de un mantenimiento, en UTC: inicio y fin (el fin siempre existe y es posterior al
 * inicio). Un mantenimiento originado por un aviso del Módulo 2 no tiene ventana.
 */
public record MaintenanceWindow(LocalDateTime start, LocalDateTime until) {

  /** Indica si el fin ya pasó respecto de {@code now}. */
  public boolean expiredAt(LocalDateTime now) {
    return !until.isAfter(now);
  }
}
