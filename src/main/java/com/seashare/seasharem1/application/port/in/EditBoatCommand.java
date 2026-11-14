package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.AdditionalServices;
import java.util.UUID;

/**
 * Datos de la edición de una embarcación. Todo llega como texto sin validar para poder dar el
 * mensaje exacto de cada campo.
 *
 * @param services servicios adicionales; nulo significa "no tocar los guardados" (por ejemplo,
 *     cuando la pantalla no pudo cargarlos)
 * @param photo foto nueva; nula significa "conservar la actual"
 */
public record EditBoatCommand(
    UUID ownerId,
    UUID boatId,
    String name,
    String maxCapacity,
    String portName,
    String latitude,
    String longitude,
    String baseRate,
    AdditionalServices services,
    byte[] photo) {}
