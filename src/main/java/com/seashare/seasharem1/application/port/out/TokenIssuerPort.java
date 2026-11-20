package com.seashare.seasharem1.application.port.out;

import com.seashare.seasharem1.domain.model.User;

/** Puerto de salida: emitir el token de acceso de un usuario autenticado. */
public interface TokenIssuerPort {

  IssuedToken issue(User user);
}
