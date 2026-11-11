CREATE TABLE frameworks (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    version     VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE requirements (
    id            BIGSERIAL PRIMARY KEY,
    framework_id  BIGINT       NOT NULL REFERENCES frameworks (id),
    clause_ref    VARCHAR(30)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    UNIQUE (framework_id, clause_ref)
);

CREATE TABLE controls (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(20)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    category    VARCHAR(20)  NOT NULL
                CHECK (category IN ('ORGANIZATIONAL', 'PEOPLE', 'PHYSICAL', 'TECHNOLOGICAL')),
    owner_id    BIGINT REFERENCES users (id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PLANNED'
                CHECK (status IN ('PLANNED', 'EFFECTIVE', 'NEEDS_REVIEW', 'INEFFECTIVE')),
    deleted_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE control_requirement (
    control_id     BIGINT      NOT NULL REFERENCES controls (id) ON DELETE CASCADE,
    requirement_id BIGINT      NOT NULL REFERENCES requirements (id) ON DELETE CASCADE,
    mapped_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (control_id, requirement_id)
);

CREATE INDEX idx_control_requirement_req ON control_requirement (requirement_id);
CREATE INDEX idx_controls_status ON controls (status) WHERE deleted_at IS NULL;
