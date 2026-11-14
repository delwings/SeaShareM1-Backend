package com.seashare.seasharem1.application.port.in;

import java.util.UUID;

/**
 * Datos del paso 1 del registro. Todo llega sin validar: el caso de uso aplica las reglas y
 * devuelve los mensajes de la especificación.
 */
public record CreateDraftCommand(
    UUID ownerId, String name, String legalRegistration, String type, Integer maxCapacity) {}
