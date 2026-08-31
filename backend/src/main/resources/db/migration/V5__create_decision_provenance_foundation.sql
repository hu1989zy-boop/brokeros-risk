CREATE TABLE decision_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decision_ref CHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_ref CHAR(39) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    conclusion_text VARBINARY(4000) NOT NULL,
    recorded_by_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_decision_record PRIMARY KEY (id),
    CONSTRAINT uk_decision_record_ref UNIQUE (decision_ref),
    CONSTRAINT ck_decision_record_ref CHECK (
        REGEXP_LIKE(decision_ref, '^dec-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_decision_record_subject_ref CHECK (
        REGEXP_LIKE(subject_ref, '^ta-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_decision_record_actor_ref CHECK (
        REGEXP_LIKE(recorded_by_actor_ref, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_decision_record_source CHECK (source IN ('MANUAL')),
    CONSTRAINT ck_decision_record_conclusion CHECK (
        OCTET_LENGTH(conclusion_text) BETWEEN 1 AND 4000),
    INDEX idx_decision_record_subject (subject_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE decision_evidence_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decision_id BIGINT NOT NULL,
    evidence_ref CHAR(39) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_decision_evidence_reference PRIMARY KEY (id),
    CONSTRAINT uk_decision_evidence_reference UNIQUE (decision_id, evidence_ref),
    CONSTRAINT fk_decision_evidence_reference_decision FOREIGN KEY (decision_id)
        REFERENCES decision_record (id) ON DELETE RESTRICT,
    CONSTRAINT ck_decision_evidence_reference_ref CHECK (
        REGEXP_LIKE(evidence_ref, '^ev-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    INDEX idx_decision_evidence_reference_decision (decision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE decision_operation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    operation_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    semantic_fingerprint BINARY(32) NOT NULL,
    decision_id BIGINT NOT NULL,
    outcome VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_decision_operation PRIMARY KEY (id),
    CONSTRAINT uk_decision_operation_id UNIQUE (operation_id),
    CONSTRAINT fk_decision_operation_record FOREIGN KEY (decision_id)
        REFERENCES decision_record (id) ON DELETE RESTRICT,
    CONSTRAINT ck_decision_operation_id CHECK (
        REGEXP_LIKE(operation_id, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_decision_operation_type CHECK (operation_type IN ('RECORD')),
    CONSTRAINT ck_decision_operation_outcome CHECK (outcome IN ('CREATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE decision_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    decision_id BIGINT NOT NULL,
    accessing_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    accessed_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_decision_access_log PRIMARY KEY (id),
    CONSTRAINT fk_decision_access_log_record FOREIGN KEY (decision_id)
        REFERENCES decision_record (id) ON DELETE RESTRICT,
    INDEX idx_decision_access_log_record (decision_id, accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
