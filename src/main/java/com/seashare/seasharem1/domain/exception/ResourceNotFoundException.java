package com.seashare.seasharem1.domain.exception;

/**
 * El recurso pedido no existe o no es visible para quien lo pide. Se usa también cuando el recurso
 * pertenece a otro propietario, para no revelar su existencia (error 404 del catálogo).
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
