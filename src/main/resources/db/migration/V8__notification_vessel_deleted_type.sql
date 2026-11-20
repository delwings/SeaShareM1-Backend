-- Nuevo tipo de notificación: VESSEL_DELETED, del sistema al propietario cuando un administrador
-- elimina su embarcación. Solo amplía el ENUM; no cambia filas existentes.
ALTER TABLE notification
    MODIFY COLUMN type ENUM ('MAINTENANCE_NOTICE', 'REVIEW_REQUEST', 'VESSEL_AVAILABLE', 'VESSEL_DELETED') NOT NULL;
