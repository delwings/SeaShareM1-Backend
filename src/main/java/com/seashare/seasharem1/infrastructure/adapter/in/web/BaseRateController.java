package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.BaseRateQuery;
import com.seashare.seasharem1.application.port.in.BaseRateQuery.BaseRates;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BaseRatesRequest;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.BaseRatesResponse;
import java.math.BigDecimal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta interna del Módulo 3: recibe un lote de identificadores y devuelve la tarifa base de
 * cada embarcación. Es un POST por el cuerpo del lote, pero no modifica nada. Sin autenticación,
 * por decisión del proyecto.
 */
@RestController
@RequestMapping("/api/v1/boats/base-rates")
public class BaseRateController {

  private static final Logger LOG = LoggerFactory.getLogger(BaseRateController.class);

  private final BaseRateQuery query;

  public BaseRateController(BaseRateQuery query) {
    this.query = query;
  }

  @PostMapping
  public BaseRatesResponse query(@RequestBody(required = false) BaseRatesRequest request) {
    BaseRates result = query.getBaseRates(request == null ? null : request.boatIds());
    if (!result.notFound().isEmpty()) {
      LOG.warn("Tarifa base: embarcaciones no encontradas {}", result.notFound());
    }
    return new BaseRatesResponse(
        result.rates().stream()
            .map(
                rate ->
                    new BaseRatesResponse.Rate(rate.boatId().toString(), format(rate.baseRate())))
            .toList(),
        result.notFound().stream().map(UUID::toString).toList());
  }

  private static String format(BigDecimal value) {
    return value.setScale(2, java.math.RoundingMode.UNNECESSARY).toPlainString();
  }
}
