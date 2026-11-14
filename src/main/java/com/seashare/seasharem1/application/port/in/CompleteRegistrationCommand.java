package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.AdditionalServices;
import java.util.UUID;

/**
 * Datos del paso 2 del registro. La tarifa y las coordenadas llegan como texto para poder dar el
 * mensaje exacto cuando traen letras, puntos o signos.
 *
 * @param photo contenido de la imagen; nulo si no se adjuntó
 */
public record CompleteRegistrationCommand(
    UUID ownerId,
    UUID boatId,
    String portName,
    String latitude,
    String longitude,
    String baseRate,
    AdditionalServices services,
    byte[] photo) {}
