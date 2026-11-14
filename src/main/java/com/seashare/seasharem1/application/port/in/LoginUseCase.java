package com.seashare.seasharem1.application.port.in;

/** Puerto de entrada: iniciar sesión con número de documento y contraseña. */
public interface LoginUseCase {

  /**
   * Autentica al usuario.
   *
   * @throws com.seashare.seasharem1.domain.exception.InvalidCredentialsException si el documento no
   *     existe o la contraseña no coincide
   */
  LoginResult login(LoginCommand command);
}
