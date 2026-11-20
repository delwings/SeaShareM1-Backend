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

  /**
   * Catálogo paginado de embarcaciones registradas (ni borrador ni eliminadas), de cualquier
   * estado, ordenadas por nombre.
   *
   * @param query texto libre sobre el nombre o el puerto; nulo o vacío = todas
   * @param page página, desde 1 (un valor menor se toma como 1)
   * @param size tamaño de página (por defecto 20, máximo 100)
   */
  CatalogPage getCatalog(String query, int page, int size);

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

  /**
   * Estado operativo con la ventana de mantenimiento ({@code maintenanceStart} y {@code
   * maintenanceUntil} son nulos si no hay). Si el fin del mantenimiento ya pasó se informa
   * Disponible y sin fechas.
   */
  record OperationalStatus(
      UUID vesselId,
      VesselStatus status,
      Instant queriedAt,
      Instant maintenanceStart,
      Instant maintenanceUntil) {}

  record CatalogItem(
      UUID id, String name, VesselType type, String photoUrl, Port port, int maxCapacity) {}

  record CatalogPage(List<CatalogItem> items, int page, int size, long totalItems) {

    public int totalPages() {
      return (int) ((totalItems + size - 1) / size);
    }
  }
}
