package com.brokeros.risk.decision.infrastructure.persistence;

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

@EnabledIfEnvironmentVariable(named = "Q012_MYSQL_TEST_URL", matches = ".+")
class Q012MySqlMigrationTests {

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
    void migrationUpgradesV4CreatesExactlyFourTablesWithoutSeedsAndValidatesOnRestart() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        assertThat(Flyway.configure().dataSource(dataSource).target("4").load()
                .migrate().migrationsExecuted).isEqualTo(4);
        assertThat(decisionTables()).isEmpty();

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(decisionTables()).containsExactly(
                "decision_access_log",
                "decision_evidence_reference",
                "decision_operation",
                "decision_record");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_evidence_reference", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM decision_access_log", Integer.class)).isZero();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex() {
        assertThat(constraints("decision_record")).contains(
                "PRIMARY",
                "uk_decision_record_ref",
                "ck_decision_record_ref",
                "ck_decision_record_subject_ref",
                "ck_decision_record_actor_ref",
                "ck_decision_record_source",
                "ck_decision_record_conclusion");
        assertThat(constraints("decision_evidence_reference")).contains(
                "PRIMARY",
                "uk_decision_evidence_reference",
                "fk_decision_evidence_reference_decision",
                "ck_decision_evidence_reference_ref");
        assertThat(constraints("decision_operation")).contains(
                "PRIMARY",
                "uk_decision_operation_id",
                "fk_decision_operation_record",
                "ck_decision_operation_id",
                "ck_decision_operation_type",
                "ck_decision_operation_outcome");
        assertThat(constraints("decision_access_log")).contains(
                "PRIMARY", "fk_decision_access_log_record");
        assertThat(indexes("decision_record")).contains(
                "PRIMARY", "uk_decision_record_ref", "idx_decision_record_subject");
        assertThat(indexes("decision_evidence_reference")).contains(
                "PRIMARY", "uk_decision_evidence_reference",
                "idx_decision_evidence_reference_decision");
        assertThat(indexes("decision_operation")).contains(
                "PRIMARY", "uk_decision_operation_id");
        assertThat(indexes("decision_access_log")).contains(
                "PRIMARY", "idx_decision_access_log_record");
        assertThat(foreignKeys()).containsExactlyInAnyOrder(
                "decision_access_log.decision_id->decision_record.id",
                "decision_evidence_reference.decision_id->decision_record.id",
                "decision_operation.decision_id->decision_record.id");
    }

    @Test
    void decisionRecordPrimaryUniqueReferenceSubjectActorSourceAndConclusionChecksAreEnforced() {
        long first = insertRecord(
                decisionRef(1), SUBJECT, "MANUAL", ACTOR, bytes("conclusion"));
        assertThat(first).isPositive();

        assertRejected(() -> insertRecord(
                decisionRef(1), SUBJECT, "MANUAL", ACTOR, bytes("duplicate")));
        assertRejected(() -> insertRecord(
                "dec-not-a-uuid", SUBJECT, "MANUAL", ACTOR, bytes("bad ref")));
        assertRejected(() -> insertRecord(
                decisionRef(2), "ta-not-a-uuid", "MANUAL", ACTOR, bytes("bad subject")));
        assertRejected(() -> insertRecord(
                decisionRef(2), SUBJECT, "MANUAL", "not-an-actor", bytes("bad actor")));
        assertRejected(() -> insertRecord(
                decisionRef(2), SUBJECT, "AUTOMATED", ACTOR, bytes("bad source")));
        assertRejected(() -> insertRecord(
                decisionRef(2), SUBJECT, "MANUAL", ACTOR, new byte[0]));
        assertRejected(() -> insertRecord(
                decisionRef(2), SUBJECT, "MANUAL", ACTOR, new byte[4001]));
    }

    @Test
    void evidenceReferencePrimaryForeignKeyUniquePairAndCanonicalShapeAreEnforced() {
        long decisionId = insertRecord(
                decisionRef(1), SUBJECT, "MANUAL", ACTOR, bytes("conclusion"));
        insertEvidenceReference(decisionId, evidenceRef(1));

        assertRejected(() -> insertEvidenceReference(decisionId, evidenceRef(1)));
        assertRejected(() -> insertEvidenceReference(decisionId, "ev-not-a-uuid"));
        assertRejected(() -> insertEvidenceReference(Long.MAX_VALUE, evidenceRef(2)));
        assertRejected(() -> jdbc.update(
                "DELETE FROM decision_record WHERE id = ?", decisionId));
    }

    @Test
    void operationPrimaryUniqueIdSingleValueEnumsAndRecordForeignKeyAreEnforced() {
        long decisionId = insertRecord(
                decisionRef(1), SUBJECT, "MANUAL", ACTOR, bytes("conclusion"));
        assertRejected(() -> insertOperation(
                "not-an-operation", "RECORD", decisionId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "CORRECT", decisionId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", decisionId, "CORRECTED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", Long.MAX_VALUE, "CREATED"));

        insertOperation(operationId(1), "RECORD", decisionId, "CREATED");
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", decisionId, "CREATED"));
        assertRejected(() -> jdbc.update(
                "DELETE FROM decision_record WHERE id = ?", decisionId));
    }

    @Test
    void accessLogPrimaryAndRecordForeignKeyRestrictAreEnforced() {
        long decisionId = insertRecord(
                decisionRef(1), SUBJECT, "MANUAL", ACTOR, bytes("conclusion"));
        assertRejected(() -> insertAccessLog(Long.MAX_VALUE));
        insertAccessLog(decisionId);
        assertRejected(() -> jdbc.update(
                "DELETE FROM decision_record WHERE id = ?", decisionId));
    }

    @Test
    void approvedQueriesUseUniqueAndSecondaryIndexesWithoutFullScans() {
        long decisionId = insertRecord(
                decisionRef(1), SUBJECT, "MANUAL", ACTOR, bytes("conclusion"));
        insertEvidenceReference(decisionId, evidenceRef(1));
        insertOperation(operationId(1), "RECORD", decisionId, "CREATED");
        insertAccessLog(decisionId);

        assertThat(explainKey(
                "SELECT * FROM decision_record WHERE decision_ref = '" + decisionRef(1) + "'"))
                .isEqualTo("uk_decision_record_ref");
        assertThat(explainKey(
                "SELECT * FROM decision_record FORCE INDEX (idx_decision_record_subject) "
                        + "WHERE subject_ref = '" + SUBJECT + "'"))
                .isEqualTo("idx_decision_record_subject");
        assertThat(explainKey(
                "SELECT * FROM decision_evidence_reference "
                        + "FORCE INDEX (idx_decision_evidence_reference_decision) "
                        + "WHERE decision_id = " + decisionId))
                .isEqualTo("idx_decision_evidence_reference_decision");
        assertThat(explainKey(
                "SELECT * FROM decision_operation WHERE operation_id = '"
                        + operationId(1) + "'"))
                .isEqualTo("uk_decision_operation_id");
        assertThat(explainKey(
                "SELECT * FROM decision_access_log FORCE INDEX (idx_decision_access_log_record) "
                        + "WHERE decision_id = " + decisionId))
                .isEqualTo("idx_decision_access_log_record");
    }

    private java.util.List<String> decisionTables() {
        return jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name LIKE 'decision_%'
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

    private java.util.List<String> foreignKeys() {
        return jdbc.queryForList("""
                SELECT CONCAT(table_name, '.', column_name, '->',
                              referenced_table_name, '.', referenced_column_name)
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND table_name LIKE 'decision_%'
                  AND referenced_table_name IS NOT NULL
                ORDER BY table_name, column_name
                """, String.class);
    }

    private long insertRecord(
            String decisionRef,
            String subjectRef,
            String source,
            String actor,
            byte[] conclusion) {
        jdbc.update("""
                INSERT INTO decision_record (
                    decision_ref, subject_ref, source, conclusion_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, decisionRef, subjectRef, source, conclusion, actor);
        return jdbc.queryForObject(
                "SELECT id FROM decision_record WHERE decision_ref = ?",
                Long.class, decisionRef);
    }

    private void insertEvidenceReference(long decisionId, String evidenceRef) {
        jdbc.update("""
                INSERT INTO decision_evidence_reference (
                    decision_id, evidence_ref, created_at)
                VALUES (?, ?, UTC_TIMESTAMP(6))
                """, decisionId, evidenceRef);
    }

    private void insertOperation(
            String operationId,
            String operationType,
            long decisionId,
            String outcome) {
        jdbc.update("""
                INSERT INTO decision_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    decision_id, outcome, occurred_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, operationId, operationType, new byte[32], decisionId, outcome);
    }

    private void insertAccessLog(long decisionId) {
        jdbc.update("""
                INSERT INTO decision_access_log (
                    decision_id, accessing_actor_ref, accessed_at)
                VALUES (?, ?, UTC_TIMESTAMP(6))
                """, decisionId, ACTOR);
    }

    private String explainKey(String sql) {
        return jdbc.queryForObject(
                "EXPLAIN " + sql,
                (resultSet, rowNumber) -> resultSet.getString("key"));
    }

    private void assertRejected(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(DataAccessException.class);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String decisionRef(int value) {
        return "dec-00000000-0000-4000-8000-" + String.format("%012d", value);
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
        source.setUrl(required("Q012_MYSQL_TEST_URL"));
        source.setUsername(required("Q012_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q012_MYSQL_TEST_PASSWORD"));
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
