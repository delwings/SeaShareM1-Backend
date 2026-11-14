package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.OperationalStatus;
import com.seashare.seasharem1.application.port.in.ReservationsVesselQuery.VesselInfo;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.OperationalStatusResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.VesselInfoResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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

  @GetMapping("/{vesselId}/operational-status")
  public OperationalStatusResponse operationalStatus(@PathVariable UUID vesselId) {
    OperationalStatus status = query.getOperationalStatus(vesselId);
    return new OperationalStatusResponse(
        status.vesselId().toString(),
        status.status().name(),
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(status.queriedAt().atZone(ZONE)));
  }
}
