package com.brokeros.risk.riskcase.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q008_MYSQL_TEST_URL", matches = ".+")
class Q008MySqlMigrationTests {

    private static final Set<String> Q008_TABLES = Set.of(
            "risk_case", "risk_case_transition_history",
            "risk_case_assignment_history", "risk_case_priority_history",
            "risk_case_evidence_association_history",
            "risk_case_decision_association",
            "risk_case_decision_selection_history",
            "risk_case_action_association_history",
            "risk_case_resolution_history",
            "risk_case_resolution_evidence_reference",
            "risk_case_resolution_action_reference",
            "risk_case_note", "audit_record");

    private DataSource dataSource;
    private JdbcTemplate jdbc;

    @BeforeEach
    void migrateDisposableDatabase() {
        dataSource = dataSource();
        Flyway flyway = Flyway.configure().dataSource(dataSource)
                .cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void migrationUpgradesV7UsingDynamicPendingCountAndCreatesExactOwnedTables() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        assertThat(Flyway.configure().dataSource(dataSource).target("7").load()
                .migrate().migrationsExecuted).isEqualTo(7);
        assertThat(q008Tables()).isEmpty();

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        int pendingMigrationCount = flyway.info().pending().length;
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
        assertThat(q008Tables()).containsExactlyInAnyOrderElementsOf(Q008_TABLES);
        Q008_TABLES.forEach(table -> assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table, Integer.class)).isZero());
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void metadataContainsAllApprovedPrimaryUniqueForeignKeyCheckAndIndexContracts() {
        Q008_TABLES.forEach(table -> assertThat(constraints(table)).contains("PRIMARY"));
        assertThat(constraints("risk_case")).contains(
                "uq_risk_case_case_number", "uq_risk_case_creation_key",
                "ck_risk_case_case_number", "ck_risk_case_subject_type",
                "ck_risk_case_intake_source", "ck_risk_case_status",
                "ck_risk_case_priority", "ck_risk_case_assignment",
                "ck_risk_case_active_assignment", "ck_risk_case_current_decision",
                "ck_risk_case_cycle", "ck_risk_case_version");
        assertThat(constraints("risk_case_resolution_history")).contains(
                "uq_risk_case_resolution_cycle", "uq_risk_case_resolution_version",
                "fk_risk_case_resolution_case", "ck_risk_case_resolution_outcome");
        assertThat(constraints("risk_case_decision_association")).contains(
                "uq_risk_case_decision_ref", "uq_risk_case_decision_version");
        assertThat(constraints("risk_case_evidence_association_history")).contains(
                "fk_risk_case_evidence_case", "fk_risk_case_evidence_prior",
                "uq_risk_case_evidence_event_ref", "ck_risk_case_evidence_shape");
        assertThat(constraints("risk_case_action_association_history")).contains(
                "fk_risk_case_action_case", "fk_risk_case_action_prior",
                "ck_risk_case_action_shape");
        assertThat(constraints("risk_case_note")).contains(
                "fk_risk_case_note_case", "fk_risk_case_note_prior",
                "uq_risk_case_note_ref");
        assertThat(foreignKeys()).hasSize(16);
        assertThat(foreignKeys()).allMatch(key -> !key.contains("->security_")
                && !key.contains("->trading_account_")
                && !key.contains("->evidence_")
                && !key.contains("->decision_")
                && !key.contains("->action_")
                && !key.startsWith("audit_record."));
    }

    @Test
    void rootChecksRejectInvalidIdentifiersCodesAssignmentAndDecisionShapes() {
        insertRoot(caseNumber(1), "OPEN", null, null, null, null, actor(1), key(1));
        assertRejected(() -> insertRoot(
                "rc-00000000-0000-4000-8000-000000000002",
                "OPEN", null, null, null, null, actor(2), key(2)));
        assertRejected(() -> insertRoot(caseNumber(2), "UNKNOWN",
                null, null, null, null, actor(2), key(2)));
        assertRejected(() -> insertRoot(caseNumber(2), "IN_REVIEW",
                null, null, null, null, actor(2), key(2)));
        assertRejected(() -> insertRoot(caseNumber(2), "OPEN",
                actor(3), null, null, null, actor(2), key(2)));
        assertRejected(() -> insertRoot(caseNumber(2), "RESOLVED",
                actor(3), actor(2), "2026-09-02 00:00:00.000000", null,
                actor(2), key(2)));
        assertRejected(() -> insertRoot(caseNumber(2), "OPEN",
                null, null, null, null, actor(1), key(1)));
    }

    @Test
    void globalPrimaryDecisionAndOneResolutionPerCycleAreEnforced() {
        long first = insertRoot(caseNumber(1), "OPEN", null, null, null,
                decision(1), actor(1), key(1));
        long second = insertRoot(caseNumber(2), "OPEN", null, null, null,
                decision(2), actor(2), key(2));
        insertDecision(first, 1, decision(1));
        assertRejected(() -> insertDecision(second, 1, decision(1)));

        insertResolution(first, 1, 2, decision(1));
        assertRejected(() -> insertResolution(first, 1, 3, decision(1)));
        insertResolution(first, 2, 4, decision(1));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM risk_case_resolution_history WHERE case_id = ?",
                Integer.class, first)).isEqualTo(2);
    }

    @Test
    void appendOnlyHistoryAndSnapshotForeignKeysRestrictDeletion() {
        long caseId = insertRoot(caseNumber(1), "OPEN", null, null, null,
                decision(1), actor(1), key(1));
        jdbc.update("""
                INSERT INTO risk_case_transition_history (
                    case_id, case_version, cycle_no, operation_code, from_status,
                    to_status, reason, actor_ref, occurred_at)
                VALUES (?, 1, 1, 'CREATE', NULL, 'OPEN', 'create', ?, UTC_TIMESTAMP(6))
                """, caseId, actor(1));
        assertRejected(() -> jdbc.update("DELETE FROM risk_case WHERE id = ?", caseId));

        jdbc.update("""
                INSERT INTO risk_case_note (
                    note_ref, case_id, case_version, content, supersedes_note_id,
                    created_by_ref, created_at)
                VALUES (?, ?, 2, 'original', NULL, ?, UTC_TIMESTAMP(6))
                """, uuid(1), caseId, actor(1));
        long noteId = jdbc.queryForObject(
                "SELECT id FROM risk_case_note WHERE note_ref = ?", Long.class, uuid(1));
        jdbc.update("""
                INSERT INTO risk_case_note (
                    note_ref, case_id, case_version, content, supersedes_note_id,
                    created_by_ref, created_at)
                VALUES (?, ?, 3, 'correction', ?, ?, UTC_TIMESTAMP(6))
                """, uuid(2), caseId, noteId, actor(1));
        assertRejected(() -> jdbc.update("DELETE FROM risk_case_note WHERE id = ?", noteId));
        assertThat(jdbc.queryForObject("SELECT content FROM risk_case_note WHERE id = ?",
                String.class, noteId)).isEqualTo("original");
    }

    @Test
    void approvedLookupAndOrderingQueriesUseDeclaredIndexes() {
        assertThat(indexes("risk_case")).contains(
                "uq_risk_case_case_number", "uq_risk_case_creation_key",
                "ix_risk_case_subject");
        assertThat(indexes("risk_case_transition_history"))
                .contains("uq_risk_case_transition_version", "ix_risk_case_transition_order");
        assertThat(indexes("risk_case_evidence_association_history"))
                .contains("uq_risk_case_evidence_event_ref", "ix_risk_case_evidence_ref");
        assertThat(indexes("risk_case_action_association_history"))
                .contains("ix_risk_case_action_ref");
        assertThat(indexes("risk_case_resolution_history"))
                .contains("uq_risk_case_resolution_cycle", "ix_risk_case_resolution_order");
        assertThat(indexes("audit_record"))
                .contains("uq_audit_record_audit_id", "ix_audit_record_target",
                        "ix_audit_record_actor");
    }

    @Test
    void exactColumnCatalogOmitsSeverityRiskLevelTeamAndExecutionPayload() {
        assertThat(columns("risk_case")).containsExactlyInAnyOrder(
                "id", "case_number", "subject_type", "subject_ref", "intake_source",
                "intake_summary", "status", "priority", "current_assignee_ref",
                "assigned_by_ref", "assigned_at", "current_decision_ref",
                "current_cycle_no", "creation_idempotency_key_hash",
                "creation_request_hash", "created_by_ref", "created_at",
                "updated_by_ref", "updated_at", "version");
        assertThat(columns("risk_case")).doesNotContain(
                "severity", "risk_level", "team_id", "execution_status", "vendor_result");
    }

    private List<String> q008Tables() {
        String placeholders = String.join(", ", Q008_TABLES.stream()
                .sorted().map(table -> "'" + table + "'").toList());
        return jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name IN (
                """ + placeholders + ") ORDER BY table_name", String.class);
    }

    private List<String> constraints(String table) {
        return jdbc.queryForList("""
                SELECT constraint_name FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private List<String> indexes(String table) {
        return jdbc.queryForList("""
                SELECT DISTINCT index_name FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private List<String> foreignKeys() {
        return jdbc.queryForList("""
                SELECT CONCAT(table_name, '.', column_name, '->',
                              referenced_table_name, '.', referenced_column_name)
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'risk_case', 'risk_case_transition_history',
                      'risk_case_assignment_history', 'risk_case_priority_history',
                      'risk_case_evidence_association_history',
                      'risk_case_decision_association',
                      'risk_case_decision_selection_history',
                      'risk_case_action_association_history',
                      'risk_case_resolution_history',
                      'risk_case_resolution_evidence_reference',
                      'risk_case_resolution_action_reference',
                      'risk_case_note', 'audit_record')
                  AND referenced_table_name IS NOT NULL
                ORDER BY table_name, column_name
                """, String.class);
    }

    private long insertRoot(
            String caseNumber,
            String status,
            String assignee,
            String assignedBy,
            String assignedAt,
            String currentDecision,
            String actor,
            byte[] creationKey) {
        jdbc.update("""
                INSERT INTO risk_case (
                    case_number, subject_type, subject_ref, intake_source,
                    intake_summary, status, priority, current_assignee_ref,
                    assigned_by_ref, assigned_at, current_decision_ref,
                    current_cycle_no, creation_idempotency_key_hash,
                    creation_request_hash, created_by_ref, created_at,
                    updated_by_ref, updated_at, version)
                VALUES (?, 'TRADING_ACCOUNT', ?, 'MANUAL', 'intake', ?, 'NORMAL',
                        ?, ?, ?, ?, 1, ?, ?, ?, UTC_TIMESTAMP(6), ?, UTC_TIMESTAMP(6), 1)
                """, caseNumber, subject(1), status, assignee, assignedBy,
                assignedAt, currentDecision, creationKey, key(99), actor, actor);
        return jdbc.queryForObject(
                "SELECT id FROM risk_case WHERE case_number = ?", Long.class, caseNumber);
    }

    private void insertDecision(long caseId, long version, String decisionRef) {
        jdbc.update("""
                INSERT INTO risk_case_decision_association (
                    case_id, case_version, decision_ref, associated_by_ref,
                    reason, associated_at)
                VALUES (?, ?, ?, ?, 'associate', UTC_TIMESTAMP(6))
                """, caseId, version, decisionRef, actor(1));
    }

    private void insertResolution(
            long caseId, int cycle, long version, String decisionRef) {
        jdbc.update("""
                INSERT INTO risk_case_resolution_history (
                    case_id, cycle_no, case_version, outcome_code, decision_ref,
                    resolution_summary, resolved_by_ref, resolved_at)
                VALUES (?, ?, ?, 'NO_RISK', ?, 'resolved', ?, UTC_TIMESTAMP(6))
                """, caseId, cycle, version, decisionRef, actor(1));
    }

    private void assertRejected(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(DataAccessException.class);
    }

    private String caseNumber(int value) {
        return "RC-00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String subject(int value) {
        return "ta-00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String decision(int value) {
        return "dec-00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String actor(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String uuid(int value) {
        return "10000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private byte[] key(int value) {
        byte[] bytes = new byte[32];
        bytes[31] = (byte) value;
        return bytes;
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q008_MYSQL_TEST_URL"));
        source.setUsername(required("Q008_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q008_MYSQL_TEST_PASSWORD"));
        return source;
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " is required");
        }
        return value;
    }
}
