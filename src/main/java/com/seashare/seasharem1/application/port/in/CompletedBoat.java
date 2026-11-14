package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;

/** Embarcación ya registrada junto con su puerto de atraque. */
public record CompletedBoat(Vessel vessel, Port port) {}
