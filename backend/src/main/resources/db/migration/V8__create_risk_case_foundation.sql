CREATE TABLE risk_case (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_number CHAR(39) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    subject_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    intake_source VARCHAR(32) NOT NULL,
    intake_summary VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    current_assignee_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    assigned_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    assigned_at DATETIME(6) NULL,
    current_decision_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    current_cycle_no INT NOT NULL,
    creation_idempotency_key_hash BINARY(32) NOT NULL,
    creation_request_hash BINARY(32) NOT NULL,
    created_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT pk_risk_case PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_case_number UNIQUE (case_number),
    CONSTRAINT uq_risk_case_creation_key UNIQUE (created_by_ref, creation_idempotency_key_hash),
    CONSTRAINT ck_risk_case_case_number CHECK (case_number REGEXP '^RC-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'),
    CONSTRAINT ck_risk_case_subject_type CHECK (subject_type IN ('TRADING_ACCOUNT')),
    CONSTRAINT ck_risk_case_subject_ref CHECK (subject_ref REGEXP '^ta-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'),
    CONSTRAINT ck_risk_case_intake_source CHECK (intake_source IN ('MANUAL', 'DECISION_DRIVEN')),
    CONSTRAINT ck_risk_case_intake_summary CHECK (CHAR_LENGTH(TRIM(intake_summary)) BETWEEN 1 AND 1000),
    CONSTRAINT ck_risk_case_status CHECK (status IN ('OPEN', 'IN_REVIEW', 'ACTION_REQUIRED', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_risk_case_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_risk_case_assignment CHECK (
        (current_assignee_ref IS NULL AND assigned_by_ref IS NULL AND assigned_at IS NULL)
        OR (current_assignee_ref IS NOT NULL AND assigned_by_ref IS NOT NULL AND assigned_at IS NOT NULL)),
    CONSTRAINT ck_risk_case_active_assignment CHECK (status NOT IN ('IN_REVIEW', 'ACTION_REQUIRED') OR current_assignee_ref IS NOT NULL),
    CONSTRAINT ck_risk_case_current_decision CHECK (status NOT IN ('ACTION_REQUIRED', 'RESOLVED', 'CLOSED') OR current_decision_ref IS NOT NULL),
    CONSTRAINT ck_risk_case_cycle CHECK (current_cycle_no >= 1),
    CONSTRAINT ck_risk_case_version CHECK (version >= 1),
    INDEX ix_risk_case_subject (subject_type, subject_ref, created_at)
) ENGINE=InnoDB;

CREATE TABLE risk_case_transition_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    cycle_no INT NOT NULL,
    operation_code VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    actor_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_transition_history PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_transition_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_transition_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_transition_version CHECK (case_version >= 1 AND cycle_no >= 1),
    CONSTRAINT ck_risk_case_transition_operation CHECK (operation_code IN ('CREATE', 'BEGIN_REVIEW', 'MARK_ACTION_REQUIRED', 'RETURN_TO_REVIEW', 'RESOLVE', 'CLOSE', 'CANCEL', 'RESUME_RESOLVED', 'REOPEN_CLOSED')),
    CONSTRAINT ck_risk_case_transition_from CHECK (from_status IS NULL OR from_status IN ('OPEN', 'IN_REVIEW', 'ACTION_REQUIRED', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_risk_case_transition_to CHECK (to_status IN ('OPEN', 'IN_REVIEW', 'ACTION_REQUIRED', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    INDEX ix_risk_case_transition_order (case_id, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_assignment_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    previous_assignee_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    new_assignee_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    assigned_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_assignment_history PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_assignment_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_assignment_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_assignment_present CHECK (previous_assignee_ref IS NOT NULL OR new_assignee_ref IS NOT NULL),
    INDEX ix_risk_case_assignment_order (case_id, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_priority_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    previous_priority VARCHAR(16) NOT NULL,
    new_priority VARCHAR(16) NOT NULL,
    changed_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_priority_history PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_priority_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_priority_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_priority_previous CHECK (previous_priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_risk_case_priority_new CHECK (new_priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_risk_case_priority_changed CHECK (previous_priority <> new_priority),
    INDEX ix_risk_case_priority_order (case_id, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_evidence_association_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    evidence_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    prior_event_id BIGINT NULL,
    replacement_evidence_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    reason VARCHAR(1000) NOT NULL,
    source VARCHAR(64) NOT NULL,
    actor_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_evidence_history PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_evidence_event_ref UNIQUE (event_ref),
    CONSTRAINT uq_risk_case_evidence_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_evidence_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT fk_risk_case_evidence_prior FOREIGN KEY (prior_event_id) REFERENCES risk_case_evidence_association_history(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_evidence_event_ref CHECK (event_ref REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'),
    CONSTRAINT ck_risk_case_evidence_type CHECK (event_type IN ('ATTACHED', 'SUPERSEDED', 'INVALIDATED', 'WITHDRAWN')),
    CONSTRAINT ck_risk_case_evidence_shape CHECK ((event_type = 'ATTACHED' AND prior_event_id IS NULL AND replacement_evidence_ref IS NULL) OR (event_type = 'SUPERSEDED' AND prior_event_id IS NOT NULL AND replacement_evidence_ref IS NOT NULL) OR (event_type IN ('INVALIDATED', 'WITHDRAWN') AND prior_event_id IS NOT NULL AND replacement_evidence_ref IS NULL)),
    INDEX ix_risk_case_evidence_ref (case_id, evidence_ref, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_decision_association (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    decision_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    associated_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    associated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_decision_association PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_decision_ref UNIQUE (decision_ref),
    CONSTRAINT uq_risk_case_decision_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_decision_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    INDEX ix_risk_case_decision_order (case_id, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_decision_selection_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    previous_decision_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    new_decision_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    selected_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    selected_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_decision_selection PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_selection_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_selection_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_selection_changed CHECK (NOT (previous_decision_ref <=> new_decision_ref)),
    INDEX ix_risk_case_selection_order (case_id, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_action_association_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    action_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    decision_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    outcome_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    prior_event_id BIGINT NULL,
    reason VARCHAR(1000) NOT NULL,
    actor_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_action_history PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_action_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_action_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT fk_risk_case_action_prior FOREIGN KEY (prior_event_id) REFERENCES risk_case_action_association_history(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_action_type CHECK (event_type IN ('ACTION_ASSOCIATED', 'OUTCOME_REFERENCED', 'WITHDRAWN')),
    CONSTRAINT ck_risk_case_action_shape CHECK ((event_type = 'ACTION_ASSOCIATED' AND outcome_ref IS NULL AND prior_event_id IS NULL) OR (event_type = 'OUTCOME_REFERENCED' AND outcome_ref IS NOT NULL AND prior_event_id IS NOT NULL) OR (event_type = 'WITHDRAWN' AND outcome_ref IS NULL AND prior_event_id IS NOT NULL)),
    INDEX ix_risk_case_action_ref (case_id, action_ref, case_version)
) ENGINE=InnoDB;

CREATE TABLE risk_case_resolution_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    case_id BIGINT NOT NULL,
    cycle_no INT NOT NULL,
    case_version BIGINT NOT NULL,
    outcome_code VARCHAR(64) NOT NULL,
    decision_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    resolution_summary VARCHAR(2000) NOT NULL,
    resolved_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    resolved_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_resolution_history PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_resolution_cycle UNIQUE (case_id, cycle_no),
    CONSTRAINT uq_risk_case_resolution_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_resolution_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_resolution_cycle CHECK (cycle_no >= 1 AND case_version >= 1),
    CONSTRAINT ck_risk_case_resolution_outcome CHECK (outcome_code IN ('RISK_CONFIRMED_ACTION_COMPLETED', 'NO_RISK', 'FALSE_POSITIVE', 'MONITORING_ONLY', 'NO_ACTION_REQUIRED')),
    CONSTRAINT ck_risk_case_resolution_summary CHECK (CHAR_LENGTH(TRIM(resolution_summary)) BETWEEN 1 AND 2000),
    INDEX ix_risk_case_resolution_order (case_id, cycle_no)
) ENGINE=InnoDB;

CREATE TABLE risk_case_resolution_evidence_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resolution_id BIGINT NOT NULL,
    evidence_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_association_event_id BIGINT NOT NULL,
    CONSTRAINT pk_risk_case_resolution_evidence PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_resolution_evidence UNIQUE (resolution_id, evidence_ref),
    CONSTRAINT fk_risk_case_resolution_evidence_header FOREIGN KEY (resolution_id) REFERENCES risk_case_resolution_history(id) ON DELETE RESTRICT,
    CONSTRAINT fk_risk_case_resolution_evidence_event FOREIGN KEY (source_association_event_id) REFERENCES risk_case_evidence_association_history(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE risk_case_resolution_action_reference (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resolution_id BIGINT NOT NULL,
    action_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    outcome_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    source_action_event_id BIGINT NOT NULL,
    CONSTRAINT pk_risk_case_resolution_action PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_resolution_action UNIQUE (resolution_id, action_ref),
    CONSTRAINT fk_risk_case_resolution_action_header FOREIGN KEY (resolution_id) REFERENCES risk_case_resolution_history(id) ON DELETE RESTRICT,
    CONSTRAINT fk_risk_case_resolution_action_event FOREIGN KEY (source_action_event_id) REFERENCES risk_case_action_association_history(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE risk_case_note (
    id BIGINT NOT NULL AUTO_INCREMENT,
    note_ref CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    case_id BIGINT NOT NULL,
    case_version BIGINT NOT NULL,
    content VARCHAR(4000) NOT NULL,
    supersedes_note_id BIGINT NULL,
    created_by_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_risk_case_note PRIMARY KEY (id),
    CONSTRAINT uq_risk_case_note_ref UNIQUE (note_ref),
    CONSTRAINT uq_risk_case_note_version UNIQUE (case_id, case_version),
    CONSTRAINT fk_risk_case_note_case FOREIGN KEY (case_id) REFERENCES risk_case(id) ON DELETE RESTRICT,
    CONSTRAINT fk_risk_case_note_prior FOREIGN KEY (supersedes_note_id) REFERENCES risk_case_note(id) ON DELETE RESTRICT,
    CONSTRAINT ck_risk_case_note_ref CHECK (note_ref REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'),
    CONSTRAINT ck_risk_case_note_content CHECK (CHAR_LENGTH(TRIM(content)) BETWEEN 1 AND 4000),
    INDEX ix_risk_case_note_order (case_id, case_version)
) ENGINE=InnoDB;

CREATE TABLE audit_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    audit_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    target_business_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    case_version BIGINT NULL,
    operation_code VARCHAR(64) NOT NULL,
    affected_ref_type VARCHAR(32) NULL,
    affected_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NULL,
    actor_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    source VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NULL,
    trace_id VARCHAR(128) NULL,
    before_state JSON NULL,
    after_state JSON NULL,
    CONSTRAINT pk_audit_record PRIMARY KEY (id),
    CONSTRAINT uq_audit_record_audit_id UNIQUE (audit_id),
    CONSTRAINT ck_audit_record_id CHECK (audit_id REGEXP '^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$'),
    CONSTRAINT ck_audit_record_case_version CHECK (case_version IS NULL OR case_version >= 1),
    INDEX ix_audit_record_target (target_type, target_id, occurred_at, id),
    INDEX ix_audit_record_actor (actor_ref, occurred_at)
) ENGINE=InnoDB;
