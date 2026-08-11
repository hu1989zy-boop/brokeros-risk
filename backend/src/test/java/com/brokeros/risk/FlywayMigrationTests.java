package com.brokeros.risk;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

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
}
