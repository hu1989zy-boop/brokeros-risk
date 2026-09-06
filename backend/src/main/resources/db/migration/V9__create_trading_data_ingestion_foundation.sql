CREATE TABLE trading_data_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    envelope_version SMALLINT NOT NULL,
    platform VARCHAR(8) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_server_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_sequence BIGINT NOT NULL,
    trading_account_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    payload BLOB NOT NULL,
    CONSTRAINT pk_trading_data_event PRIMARY KEY (id),
    CONSTRAINT uq_trading_data_event_source_sequence
        UNIQUE (source_server_id, source_sequence),
    CONSTRAINT ck_trading_data_event_version CHECK (envelope_version >= 1),
    CONSTRAINT ck_trading_data_event_platform CHECK (platform IN ('MT4', 'MT5')),
    CONSTRAINT ck_trading_data_event_source_sequence CHECK (source_sequence >= 0),
    CONSTRAINT ck_trading_data_event_payload
        CHECK (OCTET_LENGTH(payload) BETWEEN 1 AND 65535),
    INDEX ix_trading_data_event_account_occurred_at
        (trading_account_id, occurred_at)
) ENGINE=InnoDB;

CREATE TABLE trading_data_ingestion_gap (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_server_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    from_sequence BIGINT NOT NULL,
    to_sequence BIGINT NOT NULL,
    detected_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_trading_data_ingestion_gap PRIMARY KEY (id),
    CONSTRAINT ck_trading_data_ingestion_gap_range
        CHECK (from_sequence >= 0 AND to_sequence >= from_sequence),
    INDEX ix_trading_data_ingestion_gap_server_detected_at
        (source_server_id, detected_at)
) ENGINE=InnoDB;
