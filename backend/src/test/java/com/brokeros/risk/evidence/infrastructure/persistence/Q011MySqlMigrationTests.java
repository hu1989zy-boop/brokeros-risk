package com.brokeros.risk.evidence.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@EnabledIfEnvironmentVariable(named = "Q011_MYSQL_TEST_URL", matches = ".+")
class Q011MySqlMigrationTests {

    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String SUBJECT = "ta-00000000-0000-4000-8000-000000000002";

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
    void migrationUpgradesV3CreatesExactlyFourTablesAndValidatesOnRestart() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        assertThat(Flyway.configure().dataSource(dataSource).target("3").load()
                .migrate().migrationsExecuted).isEqualTo(3);
        assertThat(evidenceTables()).isEmpty();

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(evidenceTables()).containsExactly(
                "evidence_access_log",
                "evidence_operation",
                "evidence_operation_history",
                "evidence_record");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_operation_history", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_access_log", Integer.class)).isZero();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void metadataContainsEveryNamedPrimaryUniqueForeignKeyCheckAndIndex() {
        assertThat(constraints("evidence_record")).contains(
                "PRIMARY",
                "uk_evidence_record_ref",
                "uk_evidence_record_supersedes",
                "fk_evidence_record_supersedes",
                "fk_evidence_record_superseded_by",
                "ck_evidence_record_ref",
                "ck_evidence_record_actor_ref",
                "ck_evidence_record_source",
                "ck_evidence_record_status",
                "ck_evidence_record_observation");
        assertThat(constraints("evidence_operation")).contains(
                "PRIMARY",
                "uk_evidence_operation_id",
                "fk_evidence_operation_record",
                "ck_evidence_operation_id",
                "ck_evidence_operation_type",
                "ck_evidence_operation_outcome");
        assertThat(constraints("evidence_operation_history")).contains(
                "PRIMARY",
                "uk_evidence_history_operation",
                "fk_evidence_history_operation",
                "ck_evidence_history_operation_type",
                "ck_evidence_history_before_status",
                "ck_evidence_history_after_status",
                "ck_evidence_history_reason");
        assertThat(constraints("evidence_access_log")).contains(
                "PRIMARY",
                "fk_evidence_access_log_record");
        assertThat(indexes("evidence_record")).contains(
                "PRIMARY", "uk_evidence_record_ref",
                "uk_evidence_record_supersedes", "idx_evidence_record_subject");
        assertThat(indexes("evidence_operation_history"))
                .contains("PRIMARY", "uk_evidence_history_operation", "idx_evidence_history_time");
        assertThat(indexes("evidence_access_log"))
                .contains("PRIMARY", "idx_evidence_access_log_record");
    }

    @Test
    void evidenceRecordChecksUniqueSupersessionAndSelfForeignKeysAreEnforced() {
        long targetId = insertRecord(evidenceRef(1), null, "ACTIVE", "MANUAL", ACTOR,
                "observation".getBytes(StandardCharsets.UTF_8));
        insertRecord(evidenceRef(2), null, "ACTIVE", "MANUAL", ACTOR,
                "second".getBytes(StandardCharsets.UTF_8));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM evidence_record WHERE supersedes_id IS NULL",
                Integer.class)).isEqualTo(2);

        assertRejected(() -> insertRecord(
                evidenceRef(1), null, "ACTIVE", "MANUAL", ACTOR, bytes("duplicate")));
        assertRejected(() -> insertRecord(
                "ev-not-a-uuid", null, "ACTIVE", "MANUAL", ACTOR, bytes("bad ref")));
        assertRejected(() -> insertRecord(
                evidenceRef(3), null, "ACTIVE", "MANUAL", "not-an-actor", bytes("bad actor")));
        assertRejected(() -> insertRecord(
                evidenceRef(3), null, "ACTIVE", "AUTOMATED", ACTOR, bytes("bad source")));
        assertRejected(() -> insertRecord(
                evidenceRef(3), null, "DELETED", "MANUAL", ACTOR, bytes("bad status")));
        assertRejected(() -> insertRecord(
                evidenceRef(3), null, "ACTIVE", "MANUAL", ACTOR, new byte[0]));
        assertRejected(() -> insertRecord(
                evidenceRef(3), null, "ACTIVE", "MANUAL", ACTOR, new byte[4001]));
        assertRejected(() -> insertRecord(
                evidenceRef(3), Long.MAX_VALUE, "ACTIVE", "MANUAL", ACTOR, bytes("dangling")));

        long replacementId = insertRecord(
                evidenceRef(3), targetId, "ACTIVE", "MANUAL", ACTOR, bytes("replacement"));
        assertRejected(() -> insertRecord(
                evidenceRef(4), targetId, "ACTIVE", "MANUAL", ACTOR, bytes("branch")));
        jdbc.update(
                "UPDATE evidence_record SET status = 'SUPERSEDED', superseded_by_id = ? WHERE id = ?",
                replacementId, targetId);
        assertRejected(() -> jdbc.update(
                "UPDATE evidence_record SET superseded_by_id = ? WHERE id = ?",
                Long.MAX_VALUE, replacementId));
        assertRejected(() -> jdbc.update("DELETE FROM evidence_record WHERE id = ?", targetId));
        assertRejected(() -> jdbc.update("DELETE FROM evidence_record WHERE id = ?", replacementId));
    }

    @Test
    void operationAndHistoryEnumsFksBijectionsReasonBoundsAndUniquenessAreEnforced() {
        long evidenceId = insertRecord(
                evidenceRef(1), null, "ACTIVE", "MANUAL", ACTOR, bytes("observation"));
        assertRejected(() -> insertOperation(
                "not-an-operation", "RECORD", evidenceId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "DELETE", evidenceId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", evidenceId, "UNCHANGED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", Long.MAX_VALUE, "CREATED"));

        long recordOperation = insertOperation(operationId(1), "RECORD", evidenceId, "CREATED");
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", evidenceId, "CREATED"));
        assertRejected(() -> insertHistory(
                Long.MAX_VALUE, "RECORD", null, null, "ACTIVE"));
        assertRejected(() -> insertHistory(
                recordOperation, "DELETE", null, null, "ACTIVE"));
        assertRejected(() -> insertHistory(
                recordOperation, "RECORD", bytes("not-null"), null, "ACTIVE"));
        assertRejected(() -> insertHistory(
                recordOperation, "RECORD", null, "ACTIVE", "ACTIVE"));
        assertRejected(() -> insertHistory(
                recordOperation, "RECORD", null, null, "SUPERSEDED"));
        insertHistory(recordOperation, "RECORD", null, null, "ACTIVE");
        assertRejected(() -> insertHistory(
                recordOperation, "RECORD", null, null, "ACTIVE"));

        long correctOperation = insertOperation(
                operationId(2), "CORRECT", evidenceId, "CORRECTED");
        assertRejected(() -> insertHistory(
                correctOperation, "CORRECT", null, "ACTIVE", "SUPERSEDED"));
        assertRejected(() -> insertHistory(
                correctOperation, "CORRECT", new byte[0], "ACTIVE", "SUPERSEDED"));
        assertRejected(() -> insertHistory(
                correctOperation, "CORRECT", new byte[1001], "ACTIVE", "SUPERSEDED"));
        assertRejected(() -> insertHistory(
                correctOperation, "CORRECT", bytes("reason"), null, "SUPERSEDED"));
        assertRejected(() -> insertHistory(
                correctOperation, "CORRECT", bytes("reason"), "INACTIVE", "SUPERSEDED"));
        assertRejected(() -> insertHistory(
                correctOperation, "CORRECT", bytes("reason"), "ACTIVE", "ACTIVE"));
        insertHistory(
                correctOperation, "CORRECT", bytes("reason"), "ACTIVE", "SUPERSEDED");

        assertRejected(() -> jdbc.update(
                "INSERT INTO evidence_access_log (evidence_id, accessing_actor_ref, accessed_at) "
                        + "VALUES (?, ?, UTC_TIMESTAMP(6))",
                Long.MAX_VALUE, ACTOR));
        jdbc.update(
                "INSERT INTO evidence_access_log (evidence_id, accessing_actor_ref, accessed_at) "
                        + "VALUES (?, ?, UTC_TIMESTAMP(6))",
                evidenceId, ACTOR);
        assertRejected(() -> jdbc.update("DELETE FROM evidence_record WHERE id = ?", evidenceId));
        assertRejected(() -> jdbc.update(
                "DELETE FROM evidence_operation WHERE id = ?", recordOperation));
    }

    @Test
    void approvedQueriesUseUniqueAndSecondaryIndexesWithoutFullScans() {
        long evidenceId = insertRecord(
                evidenceRef(1), null, "ACTIVE", "MANUAL", ACTOR, bytes("observation"));
        insertOperation(operationId(1), "RECORD", evidenceId, "CREATED");
        jdbc.update(
                "INSERT INTO evidence_access_log (evidence_id, accessing_actor_ref, accessed_at) "
                        + "VALUES (?, ?, UTC_TIMESTAMP(6))",
                evidenceId, ACTOR);

        assertThat(explainKey(
                "SELECT * FROM evidence_record WHERE evidence_ref = '" + evidenceRef(1) + "'"))
                .isEqualTo("uk_evidence_record_ref");
        assertThat(explainKey(
                "SELECT * FROM evidence_record FORCE INDEX (idx_evidence_record_subject) "
                        + "WHERE subject_ref = '" + SUBJECT + "'"))
                .isEqualTo("idx_evidence_record_subject");
        assertThat(explainKey(
                "SELECT * FROM evidence_operation WHERE operation_id = '" + operationId(1) + "'"))
                .isEqualTo("uk_evidence_operation_id");
        assertThat(explainKey(
                "SELECT * FROM evidence_access_log FORCE INDEX (idx_evidence_access_log_record) "
                        + "WHERE evidence_id = " + evidenceId))
                .isEqualTo("idx_evidence_access_log_record");
    }

    private java.util.List<String> evidenceTables() {
        return jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name LIKE 'evidence_%'
                ORDER BY table_name
                """, String.class);
    }

    private java.util.List<String> constraints(String table) {
        return jdbc.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private java.util.List<String> indexes(String table) {
        return jdbc.queryForList("""
                SELECT DISTINCT index_name
                FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private long insertRecord(
            String evidenceRef,
            Long supersedesId,
            String status,
            String source,
            String actor,
            byte[] observation) {
        jdbc.update("""
                INSERT INTO evidence_record (
                    evidence_ref, subject_ref, source, observation_text, status,
                    supersedes_id, superseded_by_id, recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, NULL, ?, UTC_TIMESTAMP(6))
                """, evidenceRef, SUBJECT, source, observation, status, supersedesId, actor);
        return jdbc.queryForObject(
                "SELECT id FROM evidence_record WHERE evidence_ref = ?",
                Long.class, evidenceRef);
    }

    private long insertOperation(
            String operationId,
            String operationType,
            long evidenceId,
            String outcome) {
        jdbc.update("""
                INSERT INTO evidence_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    evidence_id, outcome, occurred_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, operationId, operationType, new byte[32], evidenceId, outcome);
        return jdbc.queryForObject(
                "SELECT id FROM evidence_operation WHERE operation_id = ?",
                Long.class, operationId);
    }

    private void insertHistory(
            long operationRowId,
            String operationType,
            byte[] reason,
            String beforeStatus,
            String afterStatus) {
        jdbc.update("""
                INSERT INTO evidence_operation_history (
                    operation_row_id, operation_type, actor_ref, capability,
                    reason, before_status, after_status, occurred_at)
                VALUES (?, ?, ?, 'evidence:test', ?, ?, ?, UTC_TIMESTAMP(6))
                """, operationRowId, operationType, ACTOR, reason, beforeStatus, afterStatus);
    }

    private String explainKey(String sql) {
        return jdbc.queryForObject(
                "EXPLAIN " + sql, (resultSet, rowNumber) -> resultSet.getString("key"));
    }

    private void assertRejected(Runnable insert) {
        assertThatThrownBy(insert::run).isInstanceOf(DataAccessException.class);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String evidenceRef(int value) {
        return "ev-00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q011_MYSQL_TEST_URL"));
        source.setUsername(required("Q011_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q011_MYSQL_TEST_PASSWORD"));
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
