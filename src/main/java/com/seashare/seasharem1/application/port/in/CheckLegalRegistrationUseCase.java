package com.seashare.seasharem1.application.port.in;

/** Verifica en tiempo real si una matrícula legal está libre. */
public interface CheckLegalRegistrationUseCase {

  /**
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException si el formato es
   *     inválido
   */
  boolean isAvailable(String legalRegistration);
}
