-- Eliminación lógica de embarcaciones (excepción autorizada al modelo físico, 7 de noviembre de 2026).
-- deleted_at: momento (UTC) de la eliminación. deleted_by: usuario que eliminó (propietario o
-- administrador). Ambos son NULL mientras la embarcación no esté eliminada.
-- La matrícula legal solo debe ser única entre embarcaciones NO eliminadas. MySQL no tiene índices
-- parciales, así que se usa una columna generada que vale la matrícula mientras la embarcación está
-- activa y NULL cuando está eliminada (los NULL no chocan en un índice único).
ALTER TABLE vessel
    ADD COLUMN deleted_at TIMESTAMP NULL AFTER registered_at,
    ADD COLUMN deleted_by CHAR(36) NULL AFTER deleted_at,
    ADD COLUMN active_legal_registration VARCHAR(30)
        GENERATED ALWAYS AS (IF(deleted, NULL, legal_registration)) STORED,
    ADD CONSTRAINT fk_vessel_deleted_by FOREIGN KEY (deleted_by) REFERENCES users (id);

ALTER TABLE vessel DROP INDEX uq_vessel_legal_registration;

ALTER TABLE vessel
    ADD CONSTRAINT uq_vessel_active_legal_registration UNIQUE (active_legal_registration);
