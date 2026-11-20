-- Marca que el propietario ya solicitó la revisión del mantenimiento puesto por el administrador.
-- Se reinicia cuando el administrador envía o termina un mantenimiento.
ALTER TABLE vessel
    ADD COLUMN review_requested BOOLEAN NOT NULL DEFAULT FALSE;
