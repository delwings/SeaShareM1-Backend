-- Integración M1-M2 v2.0 (19 de noviembre de 2026).
-- Ventana del mantenimiento iniciado por el propietario o el administrador (UTC).
ALTER TABLE vessel
    ADD COLUMN maintenance_start DATETIME(6) NULL,
    ADD COLUMN maintenance_until DATETIME(6) NULL;

-- Avisos de mantenimiento pendientes de enviar al Módulo 2 (outbox). Se escriben en la misma
-- transacción que el cambio de estado y un proceso en segundo plano los entrega con reintentos.
CREATE TABLE maintenance_event_outbox (
    event_id          CHAR(36)     NOT NULL,
    vessel_id         CHAR(36)     NOT NULL,
    maintenance_start DATETIME(6)  NOT NULL,
    maintenance_until DATETIME(6)  NOT NULL,
    occurred_at       DATETIME(6)  NOT NULL,
    reason            ENUM ('OWNER', 'ADMIN') NOT NULL,
    status            ENUM ('PENDING', 'SENT', 'FAILED') NOT NULL DEFAULT 'PENDING',
    attempts          INT          NOT NULL DEFAULT 0,
    next_attempt_at   DATETIME(6)  NOT NULL,
    last_error        VARCHAR(500) NULL,
    sent_at           DATETIME(6)  NULL,
    PRIMARY KEY (event_id),
    INDEX idx_outbox_pending (status, next_attempt_at),
    CONSTRAINT fk_outbox_vessel FOREIGN KEY (vessel_id) REFERENCES vessel (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
