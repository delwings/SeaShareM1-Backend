package com.seashare.seasharem1.application.port.out;

import com.seashare.seasharem1.domain.model.Vessel;
import java.util.List;

/** Una página del catálogo de embarcaciones y el total de resultados. */
public record VesselPage(List<Vessel> items, long totalItems) {}
