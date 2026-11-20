package com.seashare.seasharem1.application.port.in;

/** Datos para iniciar sesión. */
public record LoginCommand(long documentNumber, String password) {}
