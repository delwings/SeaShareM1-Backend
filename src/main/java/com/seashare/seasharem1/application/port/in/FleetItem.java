package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.User;
import com.seashare.seasharem1.domain.model.Vessel;

/** Una fila del listado general de flota: la embarcación y su propietario. */
public record FleetItem(Vessel vessel, User owner) {}
