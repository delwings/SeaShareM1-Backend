package com.seashare.seasharem1.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del JWT, leída de app.jwt.* (variables JWT_SECRET y JWT_EXPIRATION_MINUTES).
 *
 * @param secret clave para firmar los tokens (al menos 32 caracteres)
 * @param expirationMinutes minutos de vida del token de acceso
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMinutes) {}
