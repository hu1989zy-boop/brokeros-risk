CREATE TABLE evidence_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evidence_ref CHAR(39) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_ref CHAR(39) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    observation_text VARBINARY(4000) NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    supersedes_id BIGINT NULL,
    superseded_by_id BIGINT NULL,
    recorded_by_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evidence_record PRIMARY KEY (id),
    CONSTRAINT uk_evidence_record_ref UNIQUE (evidence_ref),
    CONSTRAINT uk_evidence_record_supersedes UNIQUE (supersedes_id),
    CONSTRAINT fk_evidence_record_supersedes FOREIGN KEY (supersedes_id)
        REFERENCES evidence_record (id) ON DELETE RESTRICT,
    CONSTRAINT fk_evidence_record_superseded_by FOREIGN KEY (superseded_by_id)
        REFERENCES evidence_record (id) ON DELETE RESTRICT,
    CONSTRAINT ck_evidence_record_ref CHECK (
        REGEXP_LIKE(evidence_ref, '^ev-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_evidence_record_actor_ref CHECK (
        REGEXP_LIKE(recorded_by_actor_ref, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_evidence_record_source CHECK (source IN ('MANUAL')),
    CONSTRAINT ck_evidence_record_status CHECK (status IN ('ACTIVE', 'SUPERSEDED')),
    CONSTRAINT ck_evidence_record_observation CHECK (
        OCTET_LENGTH(observation_text) BETWEEN 1 AND 4000),
    INDEX idx_evidence_record_subject (subject_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE evidence_operation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    operation_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    semantic_fingerprint BINARY(32) NOT NULL,
    evidence_id BIGINT NOT NULL,
    outcome VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evidence_operation PRIMARY KEY (id),
    CONSTRAINT uk_evidence_operation_id UNIQUE (operation_id),
    CONSTRAINT fk_evidence_operation_record FOREIGN KEY (evidence_id)
        REFERENCES evidence_record (id) ON DELETE RESTRICT,
    CONSTRAINT ck_evidence_operation_id CHECK (
        REGEXP_LIKE(operation_id, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_evidence_operation_type CHECK (
        operation_type IN ('RECORD', 'CORRECT')),
    CONSTRAINT ck_evidence_operation_outcome CHECK (
        outcome IN ('CREATED', 'CORRECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE evidence_operation_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_row_id BIGINT NOT NULL,
    operation_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    capability VARCHAR(127) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARBINARY(1000) NULL,
    before_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL,
    after_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evidence_operation_history PRIMARY KEY (id),
    CONSTRAINT uk_evidence_history_operation UNIQUE (operation_row_id),
    CONSTRAINT fk_evidence_history_operation FOREIGN KEY (operation_row_id)
        REFERENCES evidence_operation (id) ON DELETE RESTRICT,
    CONSTRAINT ck_evidence_history_operation_type CHECK (
        operation_type IN ('RECORD', 'CORRECT')),
    CONSTRAINT ck_evidence_history_before_status CHECK (
        (operation_type = 'RECORD' AND before_status IS NULL)
        OR
        (operation_type = 'CORRECT'
            AND before_status IS NOT NULL
            AND before_status = 'ACTIVE')),
    CONSTRAINT ck_evidence_history_after_status CHECK (
        (operation_type = 'RECORD' AND after_status = 'ACTIVE')
        OR
        (operation_type = 'CORRECT' AND after_status = 'SUPERSEDED')),
    CONSTRAINT ck_evidence_history_reason CHECK (
        (operation_type = 'RECORD' AND reason IS NULL)
        OR
        (operation_type = 'CORRECT'
            AND reason IS NOT NULL
            AND OCTET_LENGTH(reason) BETWEEN 1 AND 1000)),
    INDEX idx_evidence_history_time (occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE evidence_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    evidence_id BIGINT NOT NULL,
    accessing_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    accessed_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_evidence_access_log PRIMARY KEY (id),
    CONSTRAINT fk_evidence_access_log_record FOREIGN KEY (evidence_id)
        REFERENCES evidence_record (id) ON DELETE RESTRICT,
    INDEX idx_evidence_access_log_record (evidence_id, accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
