CREATE TABLE action_outcome_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    action_outcome_ref CHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    action_ref CHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    outcome_text VARBINARY(4000) NOT NULL,
    recorded_by_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_action_outcome_record PRIMARY KEY (id),
    CONSTRAINT uk_action_outcome_record_ref UNIQUE (action_outcome_ref),
    CONSTRAINT ck_action_outcome_record_ref CHECK (
        REGEXP_LIKE(action_outcome_ref, '^aoc-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_action_outcome_record_action_ref CHECK (
        REGEXP_LIKE(action_ref, '^act-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_action_outcome_record_actor_ref CHECK (
        REGEXP_LIKE(recorded_by_actor_ref, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_action_outcome_record_source CHECK (source IN ('MANUAL')),
    CONSTRAINT ck_action_outcome_record_text CHECK (
        OCTET_LENGTH(outcome_text) BETWEEN 1 AND 4000),
    INDEX idx_action_outcome_record_action (action_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE action_outcome_operation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    operation_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    semantic_fingerprint BINARY(32) NOT NULL,
    action_outcome_id BIGINT NOT NULL,
    outcome VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_action_outcome_operation PRIMARY KEY (id),
    CONSTRAINT uk_action_outcome_operation_id UNIQUE (operation_id),
    CONSTRAINT fk_action_outcome_operation_record FOREIGN KEY (action_outcome_id)
        REFERENCES action_outcome_record (id) ON DELETE RESTRICT,
    CONSTRAINT ck_action_outcome_operation_id CHECK (
        REGEXP_LIKE(operation_id, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_action_outcome_operation_type CHECK (
        operation_type IN ('RECORD')),
    CONSTRAINT ck_action_outcome_operation_outcome CHECK (
        outcome IN ('CREATED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE action_outcome_access_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    action_outcome_id BIGINT NOT NULL,
    accessing_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    accessed_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_action_outcome_access_log PRIMARY KEY (id),
    CONSTRAINT fk_action_outcome_access_log_record FOREIGN KEY (action_outcome_id)
        REFERENCES action_outcome_record (id) ON DELETE RESTRICT,
    INDEX idx_action_outcome_access_log_record (action_outcome_id, accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
