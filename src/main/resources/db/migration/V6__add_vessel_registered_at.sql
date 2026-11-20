-- Excepción autorizada al modelo físico (decisión del 4 de noviembre de 2026).
-- registered_at: momento en que la embarcación pasó a AVAILABLE por primera vez, al completar el
-- paso 2 del registro. Es NULL mientras sea borrador. Sirve para ordenar la lista de embarcaciones
-- y para la etiqueta «NUEVA» (168 horas). Se guarda en UTC.
ALTER TABLE vessel
    ADD COLUMN registered_at TIMESTAMP NULL AFTER deleted;
