-- Tabla users. Los UUID se guardan como CHAR(36) (texto legible).
CREATE TABLE users (
    id              CHAR(36)     NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    document_number BIGINT       NOT NULL,
    email           VARCHAR(150) NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    password        VARCHAR(255) NOT NULL,
    role            ENUM ('OWNER', 'ADMIN') NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_document_number UNIQUE (document_number),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
