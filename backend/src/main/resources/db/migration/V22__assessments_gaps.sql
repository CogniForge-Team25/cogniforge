CREATE TABLE assessments (
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(150) NOT NULL,
    framework_id BIGINT       NOT NULL REFERENCES frameworks (id),
    created_by   BIGINT       NOT NULL REFERENCES users (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE checklist_items (
    id              BIGSERIAL PRIMARY KEY,
    assessment_id   BIGINT       NOT NULL REFERENCES assessments (id) ON DELETE CASCADE,
    requirement_id  BIGINT       NOT NULL REFERENCES requirements (id),
    result          VARCHAR(10)  CHECK (result IN ('PASS', 'FAIL')),
    assessed_at     TIMESTAMPTZ,
    assessed_by     BIGINT REFERENCES users (id),
    UNIQUE (assessment_id, requirement_id)
);

CREATE TABLE compliance_gaps (
    id                BIGSERIAL PRIMARY KEY,
    checklist_item_id BIGINT       NOT NULL UNIQUE REFERENCES checklist_items (id),
    description       VARCHAR(255) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
                      CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_gaps_open ON compliance_gaps (status) WHERE status = 'OPEN';
