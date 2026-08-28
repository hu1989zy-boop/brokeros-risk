CREATE TABLE trading_account_authority_scope (
    id BIGINT NOT NULL AUTO_INCREMENT,
    authority_scope_ref CHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    lifecycle_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    registration_attestation_source VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    registration_attestation_ref VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    registered_by_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    last_operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_trading_account_authority_scope PRIMARY KEY (id),
    CONSTRAINT uk_ta_authority_scope_ref UNIQUE (authority_scope_ref),
    CONSTRAINT uk_ta_authority_scope_attestation UNIQUE (
        registration_attestation_source, registration_attestation_ref),
    CONSTRAINT ck_ta_scope_ref CHECK (
        REGEXP_LIKE(authority_scope_ref, '^aas-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_ta_scope_lifecycle CHECK (lifecycle_status IN ('ACTIVE', 'INACTIVE', 'RETIRED')),
    CONSTRAINT ck_ta_scope_version CHECK (version >= 0),
    CONSTRAINT ck_ta_scope_attestation_source CHECK (
        REGEXP_LIKE(registration_attestation_source, '^[a-z][a-z0-9-]{0,31}$', 'c')),
    CONSTRAINT ck_ta_scope_attestation_ref CHECK (CHAR_LENGTH(registration_attestation_ref) BETWEEN 1 AND 128),
    CONSTRAINT ck_ta_scope_actor_ref CHECK (
        REGEXP_LIKE(registered_by_actor_ref, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_ta_scope_operation_id CHECK (
        REGEXP_LIKE(last_operation_id, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    INDEX idx_ta_authority_scope_lifecycle (lifecycle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE trading_account_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    trading_account_ref CHAR(39) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    authority_scope_id BIGINT NOT NULL,
    source_family VARCHAR(63) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_instance VARCHAR(63) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_server VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_environment VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    external_account_key VARBINARY(512) NOT NULL,
    lifecycle_status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    registration_attestation_source VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    registration_attestation_ref VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    registered_by_actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    last_operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_trading_account_reference PRIMARY KEY (id),
    CONSTRAINT uk_trading_account_reference_ref UNIQUE (trading_account_ref),
    CONSTRAINT uk_trading_account_reference_external_identity UNIQUE (
        authority_scope_id, source_family, source_instance, source_server,
        source_environment, external_account_key),
    CONSTRAINT fk_ta_reference_scope FOREIGN KEY (authority_scope_id)
        REFERENCES trading_account_authority_scope (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ta_reference_ref CHECK (
        REGEXP_LIKE(trading_account_ref, '^ta-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_ta_reference_family CHECK (REGEXP_LIKE(source_family, '^[a-z][a-z0-9-]{0,62}$', 'c')),
    CONSTRAINT ck_ta_reference_instance CHECK (REGEXP_LIKE(source_instance, '^[a-z][a-z0-9-]{0,62}$', 'c')),
    CONSTRAINT ck_ta_reference_server CHECK (REGEXP_LIKE(source_server, '^[a-z0-9][a-z0-9._-]{0,127}$', 'c')),
    CONSTRAINT ck_ta_reference_environment CHECK (REGEXP_LIKE(source_environment, '^[a-z][a-z0-9-]{0,31}$', 'c')),
    CONSTRAINT ck_ta_reference_key CHECK (OCTET_LENGTH(external_account_key) BETWEEN 1 AND 512),
    CONSTRAINT ck_ta_reference_lifecycle CHECK (lifecycle_status IN ('ACTIVE', 'INACTIVE', 'RETIRED')),
    CONSTRAINT ck_ta_reference_version CHECK (version >= 0),
    CONSTRAINT ck_ta_reference_attestation_source CHECK (
        REGEXP_LIKE(registration_attestation_source, '^[a-z][a-z0-9-]{0,31}$', 'c')),
    CONSTRAINT ck_ta_reference_attestation_ref CHECK (CHAR_LENGTH(registration_attestation_ref) BETWEEN 1 AND 128),
    CONSTRAINT ck_ta_reference_actor_ref CHECK (
        REGEXP_LIKE(registered_by_actor_ref, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_ta_reference_operation_id CHECK (
        REGEXP_LIKE(last_operation_id, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    INDEX idx_ta_reference_scope_lifecycle (authority_scope_id, lifecycle_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE trading_account_authority_operation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    schema_version SMALLINT UNSIGNED NOT NULL,
    operation_type VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    semantic_fingerprint BINARY(32) NOT NULL,
    target_type VARCHAR(24) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    authority_scope_id BIGINT NULL,
    trading_account_id BIGINT NULL,
    target_ref VARCHAR(40) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    outcome VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    resulting_version BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_trading_account_authority_operation PRIMARY KEY (id),
    CONSTRAINT uk_ta_authority_operation_id UNIQUE (operation_id),
    CONSTRAINT fk_ta_operation_scope FOREIGN KEY (authority_scope_id)
        REFERENCES trading_account_authority_scope (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ta_operation_account FOREIGN KEY (trading_account_id)
        REFERENCES trading_account_reference (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ta_operation_id CHECK (
        REGEXP_LIKE(operation_id, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_ta_operation_schema CHECK (schema_version = 1),
    CONSTRAINT ck_ta_operation_type CHECK (operation_type IN (
        'REGISTER_AUTHORITY_SCOPE', 'REGISTER_TRADING_ACCOUNT',
        'DEACTIVATE_AUTHORITY_SCOPE', 'REACTIVATE_AUTHORITY_SCOPE', 'RETIRE_AUTHORITY_SCOPE',
        'DEACTIVATE_TRADING_ACCOUNT', 'REACTIVATE_TRADING_ACCOUNT', 'RETIRE_TRADING_ACCOUNT')),
    CONSTRAINT ck_ta_operation_target CHECK (
        (target_type = 'AUTHORITY_SCOPE' AND authority_scope_id IS NOT NULL
            AND trading_account_id IS NULL
            AND REGEXP_LIKE(target_ref, '^aas-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c'))
        OR
        (target_type = 'TRADING_ACCOUNT' AND authority_scope_id IS NULL
            AND trading_account_id IS NOT NULL
            AND REGEXP_LIKE(target_ref, '^ta-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c'))),
    CONSTRAINT ck_ta_operation_outcome CHECK (outcome IN ('CREATED', 'UPDATED', 'UNCHANGED')),
    CONSTRAINT ck_ta_operation_version CHECK (resulting_version >= 0),
    INDEX idx_ta_operation_scope_time (authority_scope_id, occurred_at, id),
    INDEX idx_ta_operation_account_time (trading_account_id, occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;

CREATE TABLE trading_account_authority_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_row_id BIGINT NOT NULL,
    actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    capability VARCHAR(127) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    authorization_evaluated_at DATETIME(6) NOT NULL,
    authorization_actor_version BIGINT NOT NULL,
    authorization_grant_version BIGINT NOT NULL,
    attestation_source VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    attestation_ref VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    change_reason VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    change_ref VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    before_lifecycle VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NULL,
    after_lifecycle VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    before_version BIGINT NULL,
    resulting_version BIGINT NOT NULL,
    request_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    trace_id CHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_trading_account_authority_history PRIMARY KEY (id),
    CONSTRAINT uk_ta_authority_history_operation UNIQUE (operation_row_id),
    CONSTRAINT fk_ta_history_operation FOREIGN KEY (operation_row_id)
        REFERENCES trading_account_authority_operation (id) ON DELETE RESTRICT,
    CONSTRAINT ck_ta_history_actor_ref CHECK (
        REGEXP_LIKE(actor_ref, '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$', 'c')),
    CONSTRAINT ck_ta_history_capability CHECK (
        REGEXP_LIKE(capability, '^[a-z][a-z0-9-]{0,62}:[a-z][a-z0-9-]{0,62}$', 'c')),
    CONSTRAINT ck_ta_history_authorization_versions CHECK (
        authorization_actor_version >= 0 AND authorization_grant_version >= 0),
    CONSTRAINT ck_ta_history_attestation_source CHECK (
        REGEXP_LIKE(attestation_source, '^[a-z][a-z0-9-]{0,31}$', 'c')),
    CONSTRAINT ck_ta_history_attestation_ref CHECK (CHAR_LENGTH(attestation_ref) BETWEEN 1 AND 128),
    CONSTRAINT ck_ta_history_change_reason CHECK (CHAR_LENGTH(change_reason) BETWEEN 1 AND 256),
    CONSTRAINT ck_ta_history_change_ref CHECK (CHAR_LENGTH(change_ref) BETWEEN 1 AND 128),
    CONSTRAINT ck_ta_history_lifecycle CHECK (
        (before_lifecycle IS NULL OR before_lifecycle IN ('ACTIVE', 'INACTIVE', 'RETIRED'))
        AND after_lifecycle IN ('ACTIVE', 'INACTIVE', 'RETIRED')),
    CONSTRAINT ck_ta_history_versions CHECK (
        resulting_version >= 0 AND (before_version IS NULL OR before_version >= 0)
        AND ((before_lifecycle IS NULL AND before_version IS NULL AND resulting_version = 0)
            OR (before_lifecycle IS NOT NULL AND before_version IS NOT NULL))),
    CONSTRAINT ck_ta_history_request_id CHECK (
        request_id IS NULL OR REGEXP_LIKE(request_id, '^[A-Za-z0-9._-]{1,128}$', 'c')),
    CONSTRAINT ck_ta_history_trace_id CHECK (
        trace_id IS NULL OR REGEXP_LIKE(trace_id, '^[0-9a-f]{32}$', 'c')),
    INDEX idx_ta_authority_history_time (occurred_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;
