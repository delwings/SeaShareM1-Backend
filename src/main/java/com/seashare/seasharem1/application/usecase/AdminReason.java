package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import java.util.List;

/** Motivo obligatorio que escribe el administrador (eliminar o enviar a mantenimiento). */
final class AdminReason {

  static final int MAX = 300;

  private AdminReason() {}

  /** Devuelve el motivo recortado o lanza el 400 del campo `reason`. */
  static String require(String reason, String missingMessage) {
    String clean = reason == null ? "" : reason.strip();
    if (clean.isEmpty()) {
      throw new ValidationFailedException(List.of(new FieldViolation("reason", missingMessage)));
    }
    if (clean.length() > MAX) {
      throw new ValidationFailedException(
          List.of(
              new FieldViolation(
                  "reason", "El motivo no puede superar los " + MAX + " caracteres.")));
    }
    return clean;
  }
}
