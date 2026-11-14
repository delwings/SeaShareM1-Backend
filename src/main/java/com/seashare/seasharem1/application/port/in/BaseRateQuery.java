package com.seashare.seasharem1.application.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Consulta interna de tarifa base por lote de embarcaciones (la usa el Módulo 3). */
public interface BaseRateQuery {

  /** Máximo de embarcaciones por lote. */
  int MAX_BATCH = 100;

  /**
   * Entrega la tarifa guardada de cada embarcación registrada del lote. Las inexistentes,
   * eliminadas o en borrador se reportan aparte, sin distinguir el motivo. Los repetidos cuentan
   * una sola vez.
   *
   * @param boatIds identificadores recibidos tal cual (texto), para validar su formato
   * @throws com.seashare.seasharem1.domain.exception.ValidationFailedException si el lote es nulo,
   *     vacío, trae más de {@link #MAX_BATCH} o algún identificador no es válido
   */
  BaseRates getBaseRates(List<String> boatIds);

  /** Tarifa base de una embarcación, exactamente como está guardada. */
  record BaseRate(UUID boatId, BigDecimal baseRate) {}

  /** Resultado: tarifas encontradas y, aparte, identificadores no encontrados. */
  record BaseRates(List<BaseRate> rates, List<UUID> notFound) {}
}
