package com.seashare.seasharem1.domain.model;

import java.util.UUID;

/**
 * Usuario del sistema. La contraseña es siempre el hash, nunca el texto plano.
 *
 * @param documentNumber número de documento, con el que se inicia sesión
 */
public record User(
    UUID id,
    String fullName,
    long documentNumber,
    String email,
    String phone,
    String password,
    UserRole role) {

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }
}
