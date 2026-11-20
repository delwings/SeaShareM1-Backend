package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.VesselStatus;
import java.util.UUID;

/**
 * Filtros del listado general de flota; todos opcionales.
 *
 * @param ownerId solo las embarcaciones de ese propietario
 * @param status solo las embarcaciones en ese estado
 * @param query texto que se busca en el nombre o la cédula del propietario
 */
public record FleetFilter(UUID ownerId, VesselStatus status, String query) {

  public static FleetFilter none() {
    return new FleetFilter(null, null, null);
  }
}
