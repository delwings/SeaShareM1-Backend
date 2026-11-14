package com.seashare.seasharem1.domain.exception;

import java.util.List;

/**
 * La operación choca con el estado actual: la embarcación no está en un estado que la permita, ya
 * existe un borrador o la matrícula ya está registrada (error 409 del catálogo). Puede traer la
 * lista de campos con error cuando el conflicto se muestra debajo de un campo.
 */
public class BusinessConflictException extends RuntimeException {

  private final List<FieldViolation> violations;

  public BusinessConflictException(String message) {
    this(message, List.of());
  }

  public BusinessConflictException(String message, List<FieldViolation> violations) {
    super(message);
    this.violations = List.copyOf(violations);
  }

  public List<FieldViolation> violations() {
    return violations;
  }
}
