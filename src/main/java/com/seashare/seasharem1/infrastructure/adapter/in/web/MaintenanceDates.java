package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Lee las fechas ISO-8601 de un cuerpo; si no se pueden leer responde 400 con el campo. */
final class MaintenanceDates {

  static final String INVALID = "La fecha no es válida (use el formato ISO-8601 con zona)";

  private MaintenanceDates() {}

  /** Nulo o vacío devuelve nulo (la regla de obligatoriedad la aplica el caso de uso). */
  static Instant parse(String field, String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(text.trim()).toInstant();
    } catch (DateTimeParseException invalid) {
      throw new ValidationFailedException(List.of(new FieldViolation(field, INVALID)));
    }
  }
}
