package com.brokeros.risk;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayMigrationTests {

    @Test
    void initialMigrationExistsWithoutBusinessTables() throws IOException {
        ClassPathResource migration = new ClassPathResource("db/migration/V1__initial_schema.sql");

        assertThat(migration.exists()).isTrue();
        assertThat(migration.getContentAsString(UTF_8))
                .contains("SELECT 1")
                .doesNotContainIgnoringCase("CREATE TABLE");
    }

    @Test
    void q009MigrationIsAdditiveAndCreatesOnlyApprovedSecurityTables() throws IOException {
        ClassPathResource migration =
                new ClassPathResource("db/migration/V2__create_security_actor_foundation.sql");
        String sql = migration.getContentAsString(UTF_8);

        assertThat(migration.exists()).isTrue();
        assertThat(sql)
                .contains(
                        "CREATE TABLE security_actor",
                        "CREATE TABLE security_principal_mapping",
                        "CREATE TABLE security_actor_capability",
                        "utf8mb4_bin",
                        "ascii_bin",
                        "ON DELETE RESTRICT",
                        "chk_security_actor_ref_uuid_v4",
                        "chk_security_actor_capability_value",
                        "chk_security_actor_capability_timestamps")
                .doesNotContainIgnoringCase(
                        "DROP TABLE",
                        "TRUNCATE TABLE",
                        "ALTER TABLE",
                        "INSERT INTO",
                        "DELETE FROM",
                        "UPDATE ");

        Matcher matcher = Pattern.compile(
                        "(?im)^\\s*CREATE\\s+TABLE\\s+([a-z0-9_]+)")
                .matcher(sql);
        java.util.Set<String> tables = new java.util.LinkedHashSet<>();
        while (matcher.find()) {
            tables.add(matcher.group(1));
        }
        assertThat(tables).containsExactly(
                "security_actor",
                "security_principal_mapping",
                "security_actor_capability");
    }
}
