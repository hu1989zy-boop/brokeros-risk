package com.brokeros.risk.actionoutcome.infrastructure.persistence;

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

@EnabledIfEnvironmentVariable(named = "Q014_MYSQL_TEST_URL", matches = ".+")
class Q014MySqlMigrationTests {

    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String ACTION =
            "act-00000000-0000-4000-8000-000000000002";

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
    void migrationUpgradesV6CreatesExactlyThreeTablesWithoutSeedsAndValidatesOnRestart() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        assertThat(Flyway.configure().dataSource(dataSource).target("6").load()
                .migrate().migrationsExecuted).isEqualTo(6);
        assertThat(actionOutcomeTables()).isEmpty();

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        int pendingMigrationCount = flyway.info().pending().length;
        assertThat(flyway.migrate().migrationsExecuted)
                .isEqualTo(pendingMigrationCount);
        assertThat(actionOutcomeTables()).containsExactly(
                "action_outcome_access_log",
                "action_outcome_operation",
                "action_outcome_record");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_operation", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_access_log", Integer.class))
                .isZero();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex() {
        assertThat(constraints("action_outcome_record")).contains(
                "PRIMARY",
                "uk_action_outcome_record_ref",
                "ck_action_outcome_record_ref",
                "ck_action_outcome_record_action_ref",
                "ck_action_outcome_record_actor_ref",
                "ck_action_outcome_record_source",
                "ck_action_outcome_record_text");
        assertThat(constraints("action_outcome_operation")).contains(
                "PRIMARY",
                "uk_action_outcome_operation_id",
                "fk_action_outcome_operation_record",
                "ck_action_outcome_operation_id",
                "ck_action_outcome_operation_type",
                "ck_action_outcome_operation_outcome");
        assertThat(constraints("action_outcome_access_log")).contains(
                "PRIMARY", "fk_action_outcome_access_log_record");
        assertThat(indexes("action_outcome_record")).contains(
                "PRIMARY",
                "uk_action_outcome_record_ref",
                "idx_action_outcome_record_action");
        assertThat(indexes("action_outcome_operation")).contains(
                "PRIMARY", "uk_action_outcome_operation_id");
        assertThat(indexes("action_outcome_access_log")).contains(
                "PRIMARY", "idx_action_outcome_access_log_record");
        assertThat(foreignKeys()).containsExactlyInAnyOrder(
                "action_outcome_access_log.action_outcome_id"
                        + "->action_outcome_record.id",
                "action_outcome_operation.action_outcome_id"
                        + "->action_outcome_record.id");
        assertThat(columns("action_outcome_record"))
                .containsExactlyInAnyOrder(
                        "id", "action_outcome_ref", "action_ref", "source",
                        "outcome_text", "recorded_by_actor_ref", "recorded_at")
                .doesNotContain("status", "result", "classification");
    }

    @Test
    void recordPrimaryUniqueReferenceActionActorSourceAndOutcomeTextChecksAreEnforced() {
        long first = insertRecord(
                actionOutcomeRef(1), ACTION, "MANUAL", ACTOR, bytes("outcome"));
        assertThat(first).isPositive();

        assertRejected(() -> insertRecord(
                actionOutcomeRef(1), ACTION, "MANUAL", ACTOR, bytes("duplicate")));
        assertRejected(() -> insertRecord(
                "aoc-not-a-uuid", ACTION, "MANUAL", ACTOR, bytes("bad ref")));
        assertRejected(() -> insertRecord(
                actionOutcomeRef(2), "act-not-a-uuid", "MANUAL", ACTOR,
                bytes("bad action")));
        assertRejected(() -> insertRecord(
                actionOutcomeRef(2), ACTION, "AUTOMATED", ACTOR,
                bytes("bad source")));
        assertRejected(() -> insertRecord(
                actionOutcomeRef(2), ACTION, "MANUAL", "not-an-actor",
                bytes("bad actor")));
        assertRejected(() -> insertRecord(
                actionOutcomeRef(2), ACTION, "MANUAL", ACTOR, new byte[0]));
        assertRejected(() -> insertRecord(
                actionOutcomeRef(2), ACTION, "MANUAL", ACTOR, new byte[4001]));
    }

    @Test
    void sameActionRefCanHaveMultipleOutcomeRows() {
        long first = insertRecord(
                actionOutcomeRef(1), ACTION, "MANUAL", ACTOR, bytes("first"));
        long second = insertRecord(
                actionOutcomeRef(2), ACTION, "MANUAL", ACTOR, bytes("second"));

        assertThat(first).isNotEqualTo(second);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_outcome_record WHERE action_ref = ?",
                Integer.class, ACTION)).isEqualTo(2);
        assertThat(indexes("action_outcome_record"))
                .noneMatch(index -> index.equals("uk_action_outcome_record_action"));
    }

    @Test
    void operationPrimaryUniqueIdSingleValueEnumsAndRecordForeignKeyAreEnforced() {
        long actionOutcomeId = insertRecord(
                actionOutcomeRef(1), ACTION, "MANUAL", ACTOR, bytes("outcome"));
        assertRejected(() -> insertOperation(
                "not-an-operation", "RECORD", actionOutcomeId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "CORRECT", actionOutcomeId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", actionOutcomeId, "CORRECTED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", Long.MAX_VALUE, "CREATED"));

        insertOperation(operationId(1), "RECORD", actionOutcomeId, "CREATED");
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", actionOutcomeId, "CREATED"));
        assertRejected(() -> jdbc.update(
                "DELETE FROM action_outcome_record WHERE id = ?", actionOutcomeId));
    }

    @Test
    void accessLogPrimaryAndRecordForeignKeyRestrictAreEnforced() {
        long actionOutcomeId = insertRecord(
                actionOutcomeRef(1), ACTION, "MANUAL", ACTOR, bytes("outcome"));
        assertRejected(() -> insertAccessLog(Long.MAX_VALUE));
        insertAccessLog(actionOutcomeId);
        assertRejected(() -> jdbc.update(
                "DELETE FROM action_outcome_record WHERE id = ?", actionOutcomeId));
    }

    @Test
    void approvedQueriesUseUniqueAndSecondaryIndexesWithoutFullScans() {
        long actionOutcomeId = insertRecord(
                actionOutcomeRef(1), ACTION, "MANUAL", ACTOR, bytes("outcome"));
        insertOperation(operationId(1), "RECORD", actionOutcomeId, "CREATED");
        insertAccessLog(actionOutcomeId);

        assertThat(explainKey(
                "SELECT * FROM action_outcome_record WHERE action_outcome_ref = '"
                        + actionOutcomeRef(1) + "'"))
                .isEqualTo("uk_action_outcome_record_ref");
        assertThat(explainKey(
                "SELECT * FROM action_outcome_record"
                        + " FORCE INDEX (idx_action_outcome_record_action)"
                        + " WHERE action_ref = '" + ACTION + "'"))
                .isEqualTo("idx_action_outcome_record_action");
        assertThat(explainKey(
                "SELECT * FROM action_outcome_operation WHERE operation_id = '"
                        + operationId(1) + "'"))
                .isEqualTo("uk_action_outcome_operation_id");
        assertThat(explainKey(
                "SELECT * FROM action_outcome_access_log"
                        + " FORCE INDEX (idx_action_outcome_access_log_record)"
                        + " WHERE action_outcome_id = " + actionOutcomeId))
                .isEqualTo("idx_action_outcome_access_log_record");
    }

    private java.util.List<String> actionOutcomeTables() {
        return jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'action_outcome_record',
                      'action_outcome_operation',
                      'action_outcome_access_log')
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

    private java.util.List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ?
                """, String.class, table);
    }

    private java.util.List<String> foreignKeys() {
        return jdbc.queryForList("""
                SELECT CONCAT(table_name, '.', column_name, '->',
                              referenced_table_name, '.', referenced_column_name)
                FROM information_schema.key_column_usage
                WHERE table_schema = DATABASE()
                  AND table_name IN (
                      'action_outcome_record',
                      'action_outcome_operation',
                      'action_outcome_access_log')
                  AND referenced_table_name IS NOT NULL
                ORDER BY table_name, column_name
                """, String.class);
    }

    private long insertRecord(
            String actionOutcomeRef,
            String actionRef,
            String source,
            String actor,
            byte[] outcomeText) {
        jdbc.update("""
                INSERT INTO action_outcome_record (
                    action_outcome_ref, action_ref, source, outcome_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, actionOutcomeRef, actionRef, source, outcomeText, actor);
        return jdbc.queryForObject(
                "SELECT id FROM action_outcome_record WHERE action_outcome_ref = ?",
                Long.class, actionOutcomeRef);
    }

    private void insertOperation(
            String operationId,
            String operationType,
            long actionOutcomeId,
            String outcome) {
        jdbc.update("""
                INSERT INTO action_outcome_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    action_outcome_id, outcome, occurred_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, operationId, operationType, new byte[32],
                actionOutcomeId, outcome);
    }

    private void insertAccessLog(long actionOutcomeId) {
        jdbc.update("""
                INSERT INTO action_outcome_access_log (
                    action_outcome_id, accessing_actor_ref, accessed_at)
                VALUES (?, ?, UTC_TIMESTAMP(6))
                """, actionOutcomeId, ACTOR);
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

    private String actionOutcomeRef(int value) {
        return "aoc-00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q014_MYSQL_TEST_URL"));
        source.setUsername(required("Q014_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q014_MYSQL_TEST_PASSWORD"));
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
