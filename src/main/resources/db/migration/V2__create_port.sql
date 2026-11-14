-- Tabla port. El nombre es único.
CREATE TABLE port (
    id        CHAR(36)      NOT NULL,
    name      VARCHAR(100)  NOT NULL,
    latitude  DECIMAL(9, 6) NOT NULL,
    longitude DECIMAL(9, 6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_port_name UNIQUE (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
