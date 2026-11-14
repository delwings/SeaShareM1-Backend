package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.Vessel;

/**
 * Una fila de la lista "Mis Embarcaciones Registradas".
 *
 * @param isNew verdadero solo para la embarcación registrada más recientemente, durante 168 horas
 */
public record BoatListItem(Vessel vessel, boolean isNew) {}
