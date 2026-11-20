package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.User;

/**
 * Resultado de un inicio de sesión correcto.
 *
 * @param accessToken token de acceso firmado
 * @param expiresInSeconds segundos que tarda el token en vencer
 * @param user usuario autenticado
 */
public record LoginResult(String accessToken, long expiresInSeconds, User user) {}
