-- Tabla vessel.
-- Paso 1 del registro (obligatorio desde el borrador): name, legal_registration, type, max_capacity.
-- Paso 2 (acepta NULL mientras status = 'DRAFT'): photo_path, port_id, base_rate, has_*.
-- maintenance_by solo tiene valor cuando la embarcación está en mantenimiento.
CREATE TABLE vessel (
    id                 CHAR(36)      NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    legal_registration VARCHAR(30)   NOT NULL,
    type               ENUM ('MOTORBOAT', 'YACHT', 'CATAMARAN', 'SAILBOAT') NOT NULL,
    max_capacity       SMALLINT      NOT NULL,
    photo_path         VARCHAR(255)  NULL,
    has_life_jackets   BOOLEAN       NULL,
    has_cooler_with_ice BOOLEAN      NULL,
    has_sound_system   BOOLEAN       NULL,
    has_fishing_gear   BOOLEAN       NULL,
    has_diving_gear    BOOLEAN       NULL,
    base_rate          DECIMAL(12, 2) NULL,
    status             ENUM ('DRAFT', 'AVAILABLE', 'RESERVED', 'MAINTENANCE', 'NAVIGATION') NOT NULL,
    maintenance_by     ENUM ('OWNER', 'ADMIN') NULL,
    port_id            CHAR(36)      NULL,
    owner_id           CHAR(36)      NOT NULL,
    deleted            BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uq_vessel_legal_registration UNIQUE (legal_registration),
    CONSTRAINT fk_vessel_port FOREIGN KEY (port_id) REFERENCES port (id),
    CONSTRAINT fk_vessel_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
