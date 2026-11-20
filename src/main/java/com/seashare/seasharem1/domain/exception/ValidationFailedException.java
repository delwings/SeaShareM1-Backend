package com.seashare.seasharem1.domain.exception;

import java.util.List;

/** Los datos enviados no cumplen las reglas de negocio (error 400 del catálogo). */
public class ValidationFailedException extends RuntimeException {

  private final List<FieldViolation> violations;

  public ValidationFailedException(List<FieldViolation> violations) {
    super("Los datos enviados no son válidos");
    this.violations = List.copyOf(violations);
  }

  public List<FieldViolation> violations() {
    return violations;
  }
}
