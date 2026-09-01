package com.brokeros.risk.action.infrastructure.persistence;

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

@EnabledIfEnvironmentVariable(named = "Q013_MYSQL_TEST_URL", matches = ".+")
class Q013MySqlMigrationTests {

    private static final String ACTOR = "00000000-0000-4000-8000-000000000001";
    private static final String DECISION =
            "dec-00000000-0000-4000-8000-000000000002";

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
    void migrationUpgradesV5CreatesExactlyThreeTablesWithoutSeedsAndValidatesOnRestart() {
        Flyway.configure().dataSource(dataSource).cleanDisabled(false).load().clean();
        assertThat(Flyway.configure().dataSource(dataSource).target("5").load()
                .migrate().migrationsExecuted).isEqualTo(5);
        assertThat(actionTables()).isEmpty();

        Flyway flyway = Flyway.configure().dataSource(dataSource).load();
        int pendingMigrationCount = flyway.info().pending().length;
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(pendingMigrationCount);
        assertThat(actionTables()).containsExactly(
                "action_access_log",
                "action_operation",
                "action_record");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_record", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_operation", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM action_access_log", Integer.class)).isZero();
        flyway.validate();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void metadataContainsEveryApprovedPrimaryUniqueForeignKeyCheckAndIndex() {
        assertThat(constraints("action_record")).contains(
                "PRIMARY",
                "uk_action_record_ref",
                "ck_action_record_ref",
                "ck_action_record_decision_ref",
                "ck_action_record_actor_ref",
                "ck_action_record_source",
                "ck_action_record_status",
                "ck_action_record_intent");
        assertThat(constraints("action_operation")).contains(
                "PRIMARY",
                "uk_action_operation_id",
                "fk_action_operation_record",
                "ck_action_operation_id",
                "ck_action_operation_type",
                "ck_action_operation_outcome");
        assertThat(constraints("action_access_log")).contains(
                "PRIMARY", "fk_action_access_log_record");
        assertThat(indexes("action_record")).contains(
                "PRIMARY", "uk_action_record_ref", "idx_action_record_decision");
        assertThat(indexes("action_operation")).contains(
                "PRIMARY", "uk_action_operation_id");
        assertThat(indexes("action_access_log")).contains(
                "PRIMARY", "idx_action_access_log_record");
        assertThat(foreignKeys()).containsExactlyInAnyOrder(
                "action_access_log.action_id->action_record.id",
                "action_operation.action_id->action_record.id");
    }

    @Test
    void actionRecordPrimaryUniqueReferenceDecisionActorSourceStatusAndIntentChecksAreEnforced() {
        long first = insertRecord(
                actionRef(1), DECISION, "MANUAL", "PROPOSED", ACTOR, bytes("intent"));
        assertThat(first).isPositive();

        assertRejected(() -> insertRecord(
                actionRef(1), DECISION, "MANUAL", "PROPOSED", ACTOR, bytes("duplicate")));
        assertRejected(() -> insertRecord(
                "act-not-a-uuid", DECISION, "MANUAL", "PROPOSED", ACTOR, bytes("bad ref")));
        assertRejected(() -> insertRecord(
                actionRef(2), "dec-not-a-uuid", "MANUAL", "PROPOSED", ACTOR,
                bytes("bad decision")));
        assertRejected(() -> insertRecord(
                actionRef(2), DECISION, "AUTOMATED", "PROPOSED", ACTOR,
                bytes("bad source")));
        assertRejected(() -> insertRecord(
                actionRef(2), DECISION, "MANUAL", "APPROVED", ACTOR,
                bytes("bad status")));
        assertRejected(() -> insertRecord(
                actionRef(2), DECISION, "MANUAL", "PROPOSED", "not-an-actor",
                bytes("bad actor")));
        assertRejected(() -> insertRecord(
                actionRef(2), DECISION, "MANUAL", "PROPOSED", ACTOR, new byte[0]));
        assertRejected(() -> insertRecord(
                actionRef(2), DECISION, "MANUAL", "PROPOSED", ACTOR, new byte[4001]));
    }

    @Test
    void operationPrimaryUniqueIdSingleValueEnumsAndRecordForeignKeyAreEnforced() {
        long actionId = insertRecord(
                actionRef(1), DECISION, "MANUAL", "PROPOSED", ACTOR, bytes("intent"));
        assertRejected(() -> insertOperation(
                "not-an-operation", "RECORD", actionId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "CORRECT", actionId, "CREATED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", actionId, "CORRECTED"));
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", Long.MAX_VALUE, "CREATED"));

        insertOperation(operationId(1), "RECORD", actionId, "CREATED");
        assertRejected(() -> insertOperation(
                operationId(1), "RECORD", actionId, "CREATED"));
        assertRejected(() -> jdbc.update(
                "DELETE FROM action_record WHERE id = ?", actionId));
    }

    @Test
    void accessLogPrimaryAndRecordForeignKeyRestrictAreEnforced() {
        long actionId = insertRecord(
                actionRef(1), DECISION, "MANUAL", "PROPOSED", ACTOR, bytes("intent"));
        assertRejected(() -> insertAccessLog(Long.MAX_VALUE));
        insertAccessLog(actionId);
        assertRejected(() -> jdbc.update(
                "DELETE FROM action_record WHERE id = ?", actionId));
    }

    @Test
    void approvedQueriesUseUniqueAndSecondaryIndexesWithoutFullScans() {
        long actionId = insertRecord(
                actionRef(1), DECISION, "MANUAL", "PROPOSED", ACTOR, bytes("intent"));
        insertOperation(operationId(1), "RECORD", actionId, "CREATED");
        insertAccessLog(actionId);

        assertThat(explainKey(
                "SELECT * FROM action_record WHERE action_ref = '" + actionRef(1) + "'"))
                .isEqualTo("uk_action_record_ref");
        assertThat(explainKey(
                "SELECT * FROM action_record FORCE INDEX (idx_action_record_decision) "
                        + "WHERE decision_ref = '" + DECISION + "'"))
                .isEqualTo("idx_action_record_decision");
        assertThat(explainKey(
                "SELECT * FROM action_operation WHERE operation_id = '"
                        + operationId(1) + "'"))
                .isEqualTo("uk_action_operation_id");
        assertThat(explainKey(
                "SELECT * FROM action_access_log FORCE INDEX (idx_action_access_log_record) "
                        + "WHERE action_id = " + actionId))
                .isEqualTo("idx_action_access_log_record");
    }

    private java.util.List<String> actionTables() {
        return jdbc.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name LIKE 'action_%'
                  AND table_name NOT LIKE 'action_outcome_%'
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
                  AND table_name LIKE 'action_%'
                  AND table_name NOT LIKE 'action_outcome_%'
                  AND referenced_table_name IS NOT NULL
                ORDER BY table_name, column_name
                """, String.class);
    }

    private long insertRecord(
            String actionRef,
            String decisionRef,
            String source,
            String status,
            String actor,
            byte[] intent) {
        jdbc.update("""
                INSERT INTO action_record (
                    action_ref, decision_ref, source, status, intent_text,
                    recorded_by_actor_ref, recorded_at)
                VALUES (?, ?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, actionRef, decisionRef, source, status, intent, actor);
        return jdbc.queryForObject(
                "SELECT id FROM action_record WHERE action_ref = ?",
                Long.class, actionRef);
    }

    private void insertOperation(
            String operationId,
            String operationType,
            long actionId,
            String outcome) {
        jdbc.update("""
                INSERT INTO action_operation (
                    operation_id, operation_type, semantic_fingerprint,
                    action_id, outcome, occurred_at)
                VALUES (?, ?, ?, ?, ?, UTC_TIMESTAMP(6))
                """, operationId, operationType, new byte[32], actionId, outcome);
    }

    private void insertAccessLog(long actionId) {
        jdbc.update("""
                INSERT INTO action_access_log (
                    action_id, accessing_actor_ref, accessed_at)
                VALUES (?, ?, UTC_TIMESTAMP(6))
                """, actionId, ACTOR);
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

    private String actionRef(int value) {
        return "act-00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private String operationId(int value) {
        return "00000000-0000-4000-8000-" + String.format("%012d", value);
    }

    private DataSource dataSource() {
        DriverManagerDataSource source = new DriverManagerDataSource();
        source.setDriverClassName("com.mysql.cj.jdbc.Driver");
        source.setUrl(required("Q013_MYSQL_TEST_URL"));
        source.setUsername(required("Q013_MYSQL_TEST_USERNAME"));
        source.setPassword(required("Q013_MYSQL_TEST_PASSWORD"));
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
