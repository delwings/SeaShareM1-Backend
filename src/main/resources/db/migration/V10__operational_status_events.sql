-- Asignar estado operativo (13 de noviembre de 2026).
-- status_changed_at: momento (UTC) del último cambio de estado; sirve para descartar avisos viejos.
-- processed_event: identificadores de avisos ya procesados, para no aplicar dos veces el mismo.
-- vessel_status_history: bitácora de cada cambio de estado, automático (aviso) o manual.
ALTER TABLE vessel
    ADD COLUMN status_changed_at DATETIME(6) NULL;

CREATE TABLE processed_event (
    event_id     VARCHAR(100) NOT NULL,
    vessel_id    CHAR(36)     NULL,
    processed_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE vessel_status_history (
    id              CHAR(36)     NOT NULL,
    vessel_id       CHAR(36)     NOT NULL,
    previous_status ENUM ('DRAFT', 'AVAILABLE', 'RESERVED', 'MAINTENANCE', 'NAVIGATION') NOT NULL,
    new_status      ENUM ('DRAFT', 'AVAILABLE', 'RESERVED', 'MAINTENANCE', 'NAVIGATION') NOT NULL,
    changed_at      DATETIME(6)  NOT NULL,
    source          ENUM ('AUTOMATIC', 'MANUAL') NOT NULL,
    event_id        VARCHAR(100) NULL,
    PRIMARY KEY (id),
    INDEX idx_status_history_vessel (vessel_id, changed_at),
    CONSTRAINT fk_status_history_vessel FOREIGN KEY (vessel_id) REFERENCES vessel (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
