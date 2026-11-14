package com.seashare.seasharem1.application.port.out;

import java.time.LocalDateTime;
import java.util.UUID;

/** Registro de avisos ya procesados (para no aplicar dos veces el mismo). */
public interface ProcessedEventRepositoryPort {

  boolean exists(String eventId);

  /** Anota el aviso; devuelve falso si ya estaba anotado (otro proceso lo anotó primero). */
  boolean register(String eventId, UUID vesselId, LocalDateTime processedAt);
}
