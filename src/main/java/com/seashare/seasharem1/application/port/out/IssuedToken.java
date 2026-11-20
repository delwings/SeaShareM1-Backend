package com.seashare.seasharem1.application.port.out;

/**
 * Token emitido.
 *
 * @param value el token firmado
 * @param expiresInSeconds segundos que tarda en vencer
 */
public record IssuedToken(String value, long expiresInSeconds) {}
