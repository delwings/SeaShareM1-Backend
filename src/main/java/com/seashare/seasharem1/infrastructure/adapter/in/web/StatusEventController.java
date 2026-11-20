package com.seashare.seasharem1.infrastructure.adapter.in.web;

import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase;
import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase.Outcome;
import com.seashare.seasharem1.application.port.in.AssignOperationalStatusUseCase.StatusEvent;
import com.seashare.seasharem1.infrastructure.adapter.in.web.dto.StatusEventRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simulador de la cola del Módulo 2: recibe por HTTP el mismo aviso que más adelante llegará por
 * RabbitMQ y lo entrega al mismo caso de uso. Sin autenticación entre módulos.
 */
@RestController
@RequestMapping("/api/v1/internal/status-events")
public class StatusEventController {

  private static final Logger LOG = LoggerFactory.getLogger(StatusEventController.class);

  private final AssignOperationalStatusUseCase useCase;

  public StatusEventController(AssignOperationalStatusUseCase useCase) {
    this.useCase = useCase;
  }

  @PostMapping
  public Map<String, String> receive(@RequestBody(required = false) StatusEventRequest request) {
    StatusEvent event =
        request == null
            ? new StatusEvent(null, null, null, null)
            : new StatusEvent(
                request.eventId(),
                request.vesselId(),
                request.operationalStatus(),
                request.occurredAt());
    Outcome outcome = useCase.assign(event);
    String eventId = request == null ? null : request.eventId();
    switch (outcome) {
      case APPLIED -> LOG.info("Estado operativo aplicado: aviso {}", eventId);
      case DUPLICATE -> LOG.info("Aviso duplicado ignorado: {}", eventId);
      default ->
          LOG.warn(
              "Aviso {} descartado ({}): embarcación {}",
              eventId,
              outcome,
              request == null ? null : request.vesselId());
    }
    return Map.of("event_id", String.valueOf(eventId), "outcome", outcome.name());
  }
}
