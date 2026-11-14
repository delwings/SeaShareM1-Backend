package com.seashare.seasharem1.infrastructure.adapter.out.persistence;

import com.seashare.seasharem1.application.port.out.ProcessedEventRepositoryPort;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Registro de avisos procesados en la tabla processed_event (clave primaria = event_id). */
@Component
public class ProcessedEventPersistenceAdapter implements ProcessedEventRepositoryPort {

  private final JdbcTemplate jdbc;

  public ProcessedEventPersistenceAdapter(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean exists(String eventId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId);
    return count != null && count > 0;
  }

  @Override
  public boolean register(String eventId, UUID vesselId, LocalDateTime processedAt) {
    try {
      jdbc.update(
          "INSERT INTO processed_event (event_id, vessel_id, processed_at) VALUES (?, ?, ?)",
          eventId,
          vesselId == null ? null : vesselId.toString(),
          processedAt);
      return true;
    } catch (DuplicateKeyException duplicate) {
      return false;
    }
  }
}
