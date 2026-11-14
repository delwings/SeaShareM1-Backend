package com.seashare.seasharem1.application.usecase;

import com.seashare.seasharem1.application.port.in.BaseRateQuery;
import com.seashare.seasharem1.application.port.out.VesselRepositoryPort;
import com.seashare.seasharem1.domain.exception.FieldViolation;
import com.seashare.seasharem1.domain.exception.ValidationFailedException;
import com.seashare.seasharem1.domain.model.Vessel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Consulta de solo lectura: no modifica nada y siempre lee la tarifa guardada en ese momento. */
public class BaseRateQueryService implements BaseRateQuery {

  private static final String FIELD = "boat_ids";

  private final VesselRepositoryPort vessels;

  public BaseRateQueryService(VesselRepositoryPort vessels) {
    this.vessels = vessels;
  }

  @Override
  public BaseRates getBaseRates(List<String> boatIds) {
    Set<UUID> ids = validate(boatIds);
    Map<UUID, Vessel> found =
        vessels.findRegisteredByIds(ids).stream()
            .collect(Collectors.toMap(Vessel::id, Function.identity()));

    List<BaseRate> rates = new ArrayList<>();
    List<UUID> notFound = new ArrayList<>();
    for (UUID id : ids) {
      Vessel vessel = found.get(id);
      if (vessel == null || vessel.baseRate() == null) {
        notFound.add(id);
      } else {
        rates.add(new BaseRate(id, vessel.baseRate()));
      }
    }
    return new BaseRates(rates, notFound);
  }

  /** Valida el lote completo y devuelve los identificadores sin repetidos, en el orden recibido. */
  private static Set<UUID> validate(List<String> boatIds) {
    if (boatIds == null || boatIds.isEmpty()) {
      throw invalid(FIELD, "El lote debe incluir al menos una embarcación.");
    }
    if (boatIds.size() > MAX_BATCH) {
      throw invalid(FIELD, "El lote no puede tener más de " + MAX_BATCH + " embarcaciones.");
    }
    Set<UUID> ids = new LinkedHashSet<>();
    List<FieldViolation> violations = new ArrayList<>();
    for (int i = 0; i < boatIds.size(); i++) {
      try {
        ids.add(UUID.fromString(boatIds.get(i).trim()));
      } catch (RuntimeException e) {
        violations.add(
            new FieldViolation(
                FIELD + "[" + i + "]", "El identificador de la embarcación no es válido."));
      }
    }
    if (!violations.isEmpty()) {
      throw new ValidationFailedException(violations);
    }
    return ids;
  }

  private static ValidationFailedException invalid(String field, String message) {
    return new ValidationFailedException(List.of(new FieldViolation(field, message)));
  }
}
