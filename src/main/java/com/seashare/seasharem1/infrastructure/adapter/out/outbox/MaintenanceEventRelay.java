package com.seashare.seasharem1.infrastructure.adapter.out.outbox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Entrega al Módulo 2 los avisos de mantenimiento guardados en el outbox (contrato v2.0, C5). Envía
 * {@code POST {M2_BASE_URL}/api/v1/internal/maintenance-events} sin autenticación, con conexión de
 * 1 s y lectura de 5 s. Hasta 5 intentos con esperas de 1, 5, 25 y 125 s ante timeout, error de
 * conexión, 503 o 5xx; un 4xx es definitivo y no se reintenta. Sin {@code M2_BASE_URL} no envía
 * nada y los avisos quedan pendientes.
 */
@Component
public class MaintenanceEventRelay {

  private static final Logger LOG = LoggerFactory.getLogger(MaintenanceEventRelay.class);

  static final String PATH = "/api/v1/internal/maintenance-events";
  static final int MAX_ATTEMPTS = 5;
  static final long[] BACKOFF_SECONDS = {1, 5, 25, 125};
  private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
  private static final int BATCH = 20;

  private final JdbcTemplate jdbc;
  private final String baseUrl;
  private final HttpClient client;

  public MaintenanceEventRelay(JdbcTemplate jdbc, @Value("${app.m2.base-url:}") String baseUrl) {
    this.jdbc = jdbc;
    this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
    this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1000)).build();
  }

  /** Revisa el outbox cada segundo y envía los avisos que ya les toca. */
  @Scheduled(fixedDelayString = "${app.m2.outbox-poll-ms:1000}")
  public void deliverPending() {
    if (baseUrl.isEmpty()) {
      return;
    }
    List<Pending> due =
        jdbc.query(
            "SELECT event_id, vessel_id, maintenance_start, maintenance_until, occurred_at, "
                + "reason, attempts FROM maintenance_event_outbox "
                + "WHERE status = 'PENDING' AND next_attempt_at <= ? "
                + "ORDER BY occurred_at LIMIT "
                + BATCH,
            (rs, row) ->
                new Pending(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getObject(3, LocalDateTime.class),
                    rs.getObject(4, LocalDateTime.class),
                    rs.getObject(5, LocalDateTime.class),
                    rs.getString(6),
                    rs.getInt(7)),
            LocalDateTime.now(ZoneOffset.UTC));
    due.forEach(this::deliver);
  }

  private void deliver(Pending event) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(baseUrl + PATH))
              .timeout(Duration.ofMillis(5000))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(payload(event)))
              .build();
      int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      if (status >= 200 && status < 300) {
        markSent(event);
      } else if (status >= 400 && status < 500 && status != 408 && status != 429) {
        markFailed(event, "Rechazado por M2 con HTTP " + status);
      } else {
        retryLater(event, "HTTP " + status);
      }
    } catch (HttpTimeoutException timeout) {
      retryLater(event, "Tiempo de espera agotado");
    } catch (IOException | RuntimeException failure) {
      retryLater(event, "Fallo de conexión: " + failure.getClass().getSimpleName());
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  /** Cuerpo del aviso; solo lleva UUID, enumerados y fechas, por lo que no necesita escapes. */
  static String payload(Pending event) {
    return "{\"event_id\":\"%s\",\"vessel_id\":\"%s\",\"maintenance_start\":\"%s\","
            .formatted(event.eventId(), event.vesselId(), local(event.start()))
        + "\"maintenance_until\":\"%s\",\"occurred_at\":\"%s\",\"reason\":\"%s\"}"
            .formatted(
                local(event.until()),
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                    event.occurredAt().atOffset(ZoneOffset.UTC)),
                event.reason());
  }

  private static String local(LocalDateTime utc) {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(BOGOTA));
  }

  private void markSent(Pending event) {
    jdbc.update(
        "UPDATE maintenance_event_outbox SET status = 'SENT', attempts = attempts + 1, "
            + "sent_at = ?, last_error = NULL WHERE event_id = ?",
        LocalDateTime.now(ZoneOffset.UTC),
        event.eventId());
    LOG.info("Aviso de mantenimiento {} entregado al Módulo 2", event.eventId());
  }

  private void markFailed(Pending event, String error) {
    jdbc.update(
        "UPDATE maintenance_event_outbox SET status = 'FAILED', attempts = attempts + 1, "
            + "last_error = ? WHERE event_id = ?",
        error,
        event.eventId());
    LOG.error("Aviso de mantenimiento {} descartado: {}", event.eventId(), error);
  }

  private void retryLater(Pending event, String error) {
    int attempt = event.attempts() + 1;
    if (attempt >= MAX_ATTEMPTS) {
      markFailed(event, "Agotó %d intentos: %s".formatted(MAX_ATTEMPTS, error));
      return;
    }
    long wait = BACKOFF_SECONDS[Math.min(attempt - 1, BACKOFF_SECONDS.length - 1)];
    jdbc.update(
        "UPDATE maintenance_event_outbox SET attempts = ?, last_error = ?, next_attempt_at = ? "
            + "WHERE event_id = ?",
        attempt,
        error,
        LocalDateTime.now(ZoneOffset.UTC).plusSeconds(wait),
        event.eventId());
    LOG.warn(
        "Aviso de mantenimiento {}: intento {} falló ({}); se reintenta en {} s",
        event.eventId(),
        attempt,
        error,
        wait);
  }

  record Pending(
      String eventId,
      String vesselId,
      LocalDateTime start,
      LocalDateTime until,
      LocalDateTime occurredAt,
      String reason,
      int attempts) {}
}
