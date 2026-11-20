package com.seashare.seasharem1.application.port.out;

/** Puerto de salida: comparar una contraseña con su hash. */
public interface PasswordVerifierPort {

  boolean matches(String rawPassword, String passwordHash);
}
