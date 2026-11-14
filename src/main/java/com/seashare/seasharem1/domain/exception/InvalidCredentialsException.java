package com.seashare.seasharem1.domain.exception;

/**
 * Credenciales inválidas. Se usa el mismo error tanto si el documento no existe como si la
 * contraseña es incorrecta, para no revelar qué documentos están registrados.
 */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Credenciales inválidas");
  }
}
