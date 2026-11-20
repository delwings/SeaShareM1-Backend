package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.CatalogPage;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.OperationalStatus;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.VesselInfo;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OperationalStatusResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.VesselCatalogResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.VesselInfoResponse;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultas internas del Módulo 2 (Reservas). Rutas y nombres de campo según su contrato. Sin
 * autenticación entre módulos, por decisión del proyecto.
 */
@RestController
@RequestMapping("/api/v1/vessels")
public class ReservationsVesselController {

  /** Zona horaria de todos los puertos (operación solo en Colombia). */
  static final String PORT_TIMEZONE = "America/Bogota";

  private static final ZoneId ZONE = ZoneId.of(PORT_TIMEZONE);

  private final ReservationsVesselQuery query;

  public ReservationsVesselController(ReservationsVesselQuery query) {
    this.query = query;
  }

  @GetMapping("/{vesselId}")
  public VesselInfoResponse info(@PathVariable UUID vesselId) {
    VesselInfo info = query.getInfo(vesselId);
    VesselInfoResponse.PortInfo port =
        info.port() == null
            ? null
            : new VesselInfoResponse.PortInfo(
                info.port().name(),
                info.port().latitude().doubleValue(),
                info.port().longitude().doubleValue(),
                PORT_TIMEZONE);
    return new VesselInfoResponse(
        info.id().toString(),
        info.name(),
        info.legalRegistration(),
        info.type().name(),
        info.maxCapacity(),
        info.status().name(),
        info.photoUrl(),
        true,
        port,
        info.amenities(),
        new VesselInfoResponse.OwnerInfo(info.ownerId().toString(), info.ownerName()));
  }

  /** Catálogo para el Módulo 2 (C2): público, paginado, con filtro de texto opcional. */
  @GetMapping
  public VesselCatalogResponse catalog(
      @RequestParam(value = "page", defaultValue = "1") int page,
      @RequestParam(value = "size", defaultValue = "20") int size,
      @RequestParam(value = "q", required = false) String q) {
    CatalogPage result = query.getCatalog(q, page, size);
    return new VesselCatalogResponse(
        result.items().stream()
            .map(
                item ->
                    new VesselCatalogResponse.Item(
                        item.id().toString(),
                        item.name(),
                        item.type().name(),
                        item.photoUrl(),
                        item.port() == null
                            ? null
                            : new VesselInfoResponse.PortInfo(
                                item.port().name(),
                                item.port().latitude().doubleValue(),
                                item.port().longitude().doubleValue(),
                                PORT_TIMEZONE),
                        item.maxCapacity(),
                        true))
            .toList(),
        new VesselCatalogResponse.Pagination(
            result.page(), result.size(), result.totalItems(), result.totalPages()));
  }

  /**
   * Estado operativo. Los parámetros {@code start_date} y {@code end_date} los envía el Módulo 2
   * pero el veredicto por fechas es suyo; aquí solo se acepta {@code timezone} (zona IANA) para
   * expresar las fechas, y si no es válida se usa America/Bogota.
   */
  @GetMapping("/{vesselId}/operational-status")
  public OperationalStatusResponse operationalStatus(
      @PathVariable UUID vesselId,
      @RequestParam(value = "timezone", required = false) String timezone) {
    ZoneId zone = zoneOrDefault(timezone);
    OperationalStatus status = query.getOperationalStatus(vesselId);
    return new OperationalStatusResponse(
        status.vesselId().toString(),
        status.status().name(),
        format(status.queriedAt(), zone),
        format(status.maintenanceStart(), zone),
        format(status.maintenanceUntil(), zone));
  }

  private static ZoneId zoneOrDefault(String timezone) {
    if (timezone == null || timezone.isBlank()) {
      return ZONE;
    }
    try {
      return ZoneId.of(timezone.trim());
    } catch (DateTimeException invalid) {
      return ZONE;
    }
  }

  private static String format(Instant instant, ZoneId zone) {
    return instant == null
        ? null
        : DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zone));
  }
}
