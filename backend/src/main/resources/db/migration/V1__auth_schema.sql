CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES
    ('SECURITY_MANAGER'),
    ('SECURITY_OFFICER'),
    ('IT_ADMINISTRATOR'),
    ('COMPLIANCE_OFFICER'),
    ('INTERNAL_ASSESSOR'),
    ('EXECUTIVE_REVIEWER');

CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    full_name             VARCHAR(150) NOT NULL,
    password_hash         VARCHAR(100),
    status                VARCHAR(20)  NOT NULL DEFAULT 'INVITED'
                          CHECK (status IN ('INVITED', 'ACTIVE', 'DISABLED')),
    role_id               BIGINT       NOT NULL REFERENCES roles (id),
    invitation_token_hash CHAR(64),
    invitation_expires_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_invitation ON users (invitation_token_hash);
