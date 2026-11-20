-- Tabla notification.
-- MAINTENANCE_NOTICE: del administrador al propietario, con el motivo.
-- REVIEW_REQUEST: del propietario al administrador.
-- VESSEL_AVAILABLE: automática, al propietario.
CREATE TABLE notification (
    id           CHAR(36)  NOT NULL,
    recipient_id CHAR(36)  NOT NULL,
    vessel_id    CHAR(36)  NOT NULL,
    type         ENUM ('MAINTENANCE_NOTICE', 'REVIEW_REQUEST', 'VESSEL_AVAILABLE') NOT NULL,
    message      TEXT      NOT NULL,
    is_read      BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_notification_vessel FOREIGN KEY (vessel_id) REFERENCES vessel (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
