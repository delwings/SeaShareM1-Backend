package com.seashare.seasharem1.domain.exception;

/** Falló un servicio externo del que depende la operación, como Cloudinary (error 502). */
public class ExternalServiceUnavailableException extends RuntimeException {

  public ExternalServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
