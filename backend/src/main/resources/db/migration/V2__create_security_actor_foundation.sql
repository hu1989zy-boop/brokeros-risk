-- Q-009 Trusted Actor and Authorization Foundation.
-- Forward-only, additive schema. Identity and capability data are provisioned
-- separately through the controlled application command.

CREATE TABLE security_actor (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    actor_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    provisioning_source VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provisioning_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_security_actor PRIMARY KEY (id),
    CONSTRAINT uk_security_actor_actor_ref UNIQUE (actor_ref),
    CONSTRAINT chk_security_actor_ref_uuid_v4 CHECK (
        actor_ref REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'
    ),
    CONSTRAINT chk_security_actor_type CHECK (actor_type IN ('HUMAN', 'SERVICE')),
    CONSTRAINT chk_security_actor_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_security_actor_version CHECK (version >= 0),
    CONSTRAINT chk_security_actor_provisioning_source CHECK (
        provisioning_source REGEXP '^[a-z][a-z0-9-]{0,31}$'
    ),
    CONSTRAINT chk_security_actor_provisioning_ref CHECK (
        CHAR_LENGTH(provisioning_ref) BETWEEN 1 AND 128
    ),
    INDEX idx_security_actor_type_status (actor_type, status)
) ENGINE=InnoDB;

CREATE TABLE security_principal_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    issuer VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    subject VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    principal_type VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    provisioning_source VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provisioning_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_security_principal_mapping PRIMARY KEY (id),
    CONSTRAINT uk_security_principal_mapping_external_key
        UNIQUE (issuer, subject, principal_type),
    CONSTRAINT fk_security_principal_mapping_actor
        FOREIGN KEY (actor_id) REFERENCES security_actor (id) ON DELETE RESTRICT,
    CONSTRAINT chk_security_principal_mapping_issuer CHECK (
        CHAR_LENGTH(issuer) BETWEEN 1 AND 255
    ),
    CONSTRAINT chk_security_principal_mapping_subject CHECK (
        CHAR_LENGTH(subject) BETWEEN 1 AND 255
    ),
    CONSTRAINT chk_security_principal_mapping_type CHECK (
        principal_type IN ('HUMAN', 'SERVICE')
    ),
    CONSTRAINT chk_security_principal_mapping_status CHECK (
        status IN ('ACTIVE', 'DISABLED')
    ),
    CONSTRAINT chk_security_principal_mapping_version CHECK (version >= 0),
    CONSTRAINT chk_security_principal_mapping_provisioning_source CHECK (
        provisioning_source REGEXP '^[a-z][a-z0-9-]{0,31}$'
    ),
    CONSTRAINT chk_security_principal_mapping_provisioning_ref CHECK (
        CHAR_LENGTH(provisioning_ref) BETWEEN 1 AND 128
    ),
    INDEX idx_security_principal_mapping_actor_status (actor_id, status)
) ENGINE=InnoDB;

CREATE TABLE security_actor_capability (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    capability VARCHAR(127) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    provisioning_source VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    provisioning_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    granted_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_security_actor_capability PRIMARY KEY (id),
    CONSTRAINT uk_security_actor_capability_actor_capability
        UNIQUE (actor_id, capability),
    CONSTRAINT fk_security_actor_capability_actor
        FOREIGN KEY (actor_id) REFERENCES security_actor (id) ON DELETE RESTRICT,
    CONSTRAINT chk_security_actor_capability_value CHECK (
        capability REGEXP '^[a-z][a-z0-9-]{0,62}:[a-z][a-z0-9-]{0,62}$'
    ),
    CONSTRAINT chk_security_actor_capability_status CHECK (
        status IN ('GRANTED', 'REVOKED')
    ),
    CONSTRAINT chk_security_actor_capability_version CHECK (version >= 0),
    CONSTRAINT chk_security_actor_capability_provisioning_source CHECK (
        provisioning_source REGEXP '^[a-z][a-z0-9-]{0,31}$'
    ),
    CONSTRAINT chk_security_actor_capability_provisioning_ref CHECK (
        CHAR_LENGTH(provisioning_ref) BETWEEN 1 AND 128
    ),
    CONSTRAINT chk_security_actor_capability_timestamps CHECK (
        (status = 'GRANTED' AND revoked_at IS NULL)
        OR (status = 'REVOKED' AND revoked_at IS NOT NULL)
    ),
    INDEX idx_security_actor_capability_actor_status (actor_id, status)
) ENGINE=InnoDB;
