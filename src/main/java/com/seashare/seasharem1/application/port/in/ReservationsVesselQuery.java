package com.seashare.seasharem1.application.port.in;

import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.VesselStatus;
import com.seashare.seasharem1.domain.model.VesselType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consultas internas del Módulo 2 (Reservas): datos oficiales de una embarcación y, por separado,
 * solo su estado operativo. Ambas son de solo lectura, sin caché.
 */
public interface ReservationsVesselQuery {

  /**
   * Datos de una embarcación registrada (pasos 1 y 2 del registro) y su estado operativo actual. No
   * incluye la tarifa base: la cotización es del Módulo 3.
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si no existe, fue
   *     eliminada o es un borrador
   */
  VesselInfo getInfo(UUID vesselId);

  /**
   * Únicamente el estado operativo en el momento de la consulta.
   *
   * @throws com.seashare.seasharem1.domain.exception.ResourceNotFoundException si no existe, fue
   *     eliminada o es un borrador
   */
  OperationalStatus getOperationalStatus(UUID vesselId);

  /** Ficha de la embarcación para el Módulo 2. {@code port} puede ser nulo si faltara el dato. */
  record VesselInfo(
      UUID id,
      String name,
      String legalRegistration,
      VesselType type,
      int maxCapacity,
      VesselStatus status,
      String photoUrl,
      Port port,
      List<String> amenities,
      UUID ownerId,
      String ownerName) {}

  record OperationalStatus(UUID vesselId, VesselStatus status, Instant queriedAt) {}
}
