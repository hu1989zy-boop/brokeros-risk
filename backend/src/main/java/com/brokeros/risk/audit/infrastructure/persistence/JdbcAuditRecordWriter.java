package com.brokeros.risk.audit.infrastructure.persistence;

import java.sql.Timestamp;

import com.brokeros.risk.audit.application.port.AuditRecordWriter;
import com.brokeros.risk.audit.domain.AuditRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditRecordWriter implements AuditRecordWriter {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditRecordWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(AuditRecord record) {
        jdbcTemplate.update("""
                INSERT INTO audit_record (
                    audit_id, target_type, target_id, target_business_ref,
                    case_version, operation_code, affected_ref_type, affected_ref,
                    actor_ref, occurred_at, reason, source, request_id, trace_id,
                    before_state, after_state)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CAST(? AS JSON), CAST(? AS JSON))
                """,
                record.auditId(), record.targetType(), record.targetId(),
                record.targetBusinessRef(), record.caseVersion(), record.operationCode(),
                record.affectedRefType(), record.affectedRef(), record.actorRef().value(),
                Timestamp.from(record.occurredAt()), record.reason(), record.source(),
                record.requestId(), record.traceId(), record.beforeState(), record.afterState());
    }
}
