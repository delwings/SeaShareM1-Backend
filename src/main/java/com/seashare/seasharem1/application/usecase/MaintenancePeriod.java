package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.MaintenanceWindow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Valida y completa la ventana de un mantenimiento (fin obligatorio, inicio opcional). */
final class MaintenancePeriod {

  static final String REQUIRED = "Este campo es obligatorio";
  static final String UNTIL_NOT_FUTURE = "La fecha de fin debe ser posterior a este momento";
  static final String UNTIL_NOT_AFTER_START = "La fecha de fin debe ser posterior a la de inicio";

  private MaintenancePeriod() {}

  /**
   * @param start inicio; nulo significa «desde ahora»
   * @param until fin; obligatorio
   * @throws ValidationFailedException si falta el fin, ya pasó o no es posterior al inicio
   */
  static MaintenanceWindow resolve(Instant start, Instant until, Clock clock) {
    LocalDateTime now = LocalDateTime.now(clock);
    List<FieldViolation> errors = new ArrayList<>();
    LocalDateTime from = start == null ? now : LocalDateTime.ofInstant(start, ZoneOffset.UTC);
    LocalDateTime to = until == null ? null : LocalDateTime.ofInstant(until, ZoneOffset.UTC);
    if (to == null) {
      errors.add(new FieldViolation("maintenance_until", REQUIRED));
    } else if (!to.isAfter(now)) {
      errors.add(new FieldViolation("maintenance_until", UNTIL_NOT_FUTURE));
    } else if (!to.isAfter(from)) {
      errors.add(new FieldViolation("maintenance_until", UNTIL_NOT_AFTER_START));
    }
    if (!errors.isEmpty()) {
      throw new ValidationFailedException(errors);
    }
    return new MaintenanceWindow(from, to);
  }
}
