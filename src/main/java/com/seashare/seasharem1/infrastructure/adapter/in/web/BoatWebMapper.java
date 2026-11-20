package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.BoatListItem;
import com.seashare.seasharem1.application.port.in.CompletedBoat;
import com.seashare.seasharem1.domain.model.AdditionalServices;
import com.seashare.seasharem1.domain.model.Port;
import com.seashare.seasharem1.domain.model.Vessel;
import com.seashare.seasharem1.domain.model.VesselType;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BoatDraftResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BoatListItemResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BoatTypeResponse;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.CompletedBoatResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Convierte entre el dominio y los DTOs de la API de embarcaciones. Hacia afuera la API usa el
 * vocabulario del contrato (boat_*) y los decimales como texto.
 */
final class BoatWebMapper {

  private BoatWebMapper() {}

  static BoatDraftResponse toDraft(Vessel vessel) {
    return new BoatDraftResponse(
        vessel.id(),
        vessel.name(),
        vessel.legalRegistration(),
        vessel.type().name(),
        vessel.maxCapacity(),
        vessel.status().name());
  }

  static BoatListItemResponse toListItem(BoatListItem item) {
    Vessel vessel = item.vessel();
    return new BoatListItemResponse(
        vessel.id(),
        vessel.name(),
        vessel.legalRegistration(),
        vessel.type().name(),
        vessel.status().name(),
        item.isNew(),
        formatUtc(vessel.registeredAt()));
  }

  static CompletedBoatResponse toCompleted(CompletedBoat completed) {
    Vessel vessel = completed.vessel();
    Port port = completed.port();
    AdditionalServices services = vessel.additionalServices();
    return new CompletedBoatResponse(
        vessel.id(),
        vessel.name(),
        vessel.legalRegistration(),
        vessel.type().name(),
        vessel.maxCapacity(),
        vessel.status().name(),
        vessel.maintenanceBy() == null ? null : vessel.maintenanceBy().name(),
        vessel.photoPath(),
        vessel.baseRate() == null
            ? null
            : vessel.baseRate().setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
        port == null
            ? null
            : new CompletedBoatResponse.PortResponse(
                port.name(), coordinate(port.latitude()), coordinate(port.longitude())),
        new CompletedBoatResponse.ServicesResponse(
            services.lifeJackets(),
            services.coolerWithIce(),
            services.soundSystem(),
            services.fishingGear(),
            services.divingGear()),
        formatUtc(vessel.registeredAt()),
        vessel.reviewRequested());
  }

  static BoatTypeResponse toType(VesselType type) {
    return new BoatTypeResponse(type.name(), typeName(type), type.maxCapacity());
  }

  /** Nombre del tipo en español, como lo muestra la interfaz. */
  static String typeName(VesselType type) {
    return switch (type) {
      case MOTORBOAT -> "Lancha";
      case YACHT -> "Yate";
      case CATAMARAN -> "Catamarán";
      case SAILBOAT -> "Velero";
    };
  }

  /** Coordenadas con 4 decimales, como texto. */
  static String coordinate(BigDecimal value) {
    return value.setScale(4, RoundingMode.HALF_UP).toPlainString();
  }

  /** Fecha en UTC en formato ISO 8601 (por ejemplo 2026-11-04T15:00:00Z), o null. */
  static String formatUtc(LocalDateTime value) {
    return value == null ? null : value.atOffset(ZoneOffset.UTC).toInstant().toString();
  }
}
