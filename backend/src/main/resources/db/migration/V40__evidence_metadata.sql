CREATE TABLE evidence (
    id              BIGSERIAL PRIMARY KEY,
    control_id      BIGINT       NOT NULL REFERENCES controls (id),
    title           VARCHAR(150) NOT NULL,
    evidence_type   VARCHAR(30)  NOT NULL
                    CHECK (evidence_type IN ('POLICY', 'REPORT', 'LOG', 'CONFIGURATION', 'OTHER')),
    collected_on    DATE         NOT NULL,
    valid_until     DATE         NOT NULL,
    review_status   VARCHAR(20)  NOT NULL DEFAULT 'SUBMITTED'
                    CHECK (review_status IN ('SUBMITTED', 'APPROVED', 'REJECTED')),
    reviewed_by     BIGINT REFERENCES users (id),
    review_comment  VARCHAR(255),
    submitted_by    BIGINT       NOT NULL REFERENCES users (id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (valid_until >= collected_on)
);

CREATE INDEX idx_evidence_control ON evidence (control_id);
CREATE INDEX idx_evidence_valid ON evidence (valid_until) WHERE review_status = 'APPROVED';
