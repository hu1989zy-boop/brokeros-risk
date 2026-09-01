package com.brokeros.risk.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.brokeros.risk.action.domain.ActionProvenanceView;
import org.junit.jupiter.api.Test;

class ActionArchitectureTests {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "org.springframework", "jakarta.servlet", "javax.sql", "java.sql",
            "com.fasterxml.jackson", "org.apache.kafka", "org.springframework.data.redis",
            "mt4", "mt5");

    @Test
    void domainAndApplicationHaveNoFrameworkPersistenceOrVendorImports() throws IOException {
        Path root = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/action");
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/domain/")
                            || path.toString().contains("/application/"))
                    .toList();
            assertThat(files).isNotEmpty();
            for (Path file : files) {
                assertThat(Files.readString(file)).as("isolation for %s", file)
                        .doesNotContain(FORBIDDEN_IMPORTS);
            }
        }
    }

    @Test
    void narrowProvenanceContractCannotExposeIntentText() {
        assertThat(Stream.of(ActionProvenanceView.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("intentText", "intent", "content", "reason");
    }

    @Test
    void moduleHasNoTransitionDeleteExecutionVendorOrRawContentBehavior() throws IOException {
        Path root = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/action");
        String all;
        try (Stream<Path> paths = Files.walk(root)) {
            all = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(ActionArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
        assertThat(all).doesNotContainIgnoringCase(
                "DELETE FROM action_", "TRUNCATE action_", "UPDATE action_",
                "APPROVED", "REJECTED", "WITHDRAWN", "Correction",
                "ActionOutcome", "Execution", "AccountControl", "RiskCase",
                "leverage", "withdrawal", "mt4", "mt5",
                "permitAllProvider", "SYSTEM_ACTOR", "X-Actor",
                "logger.info", "logger.debug");
    }

    @Test
    void futureRiskCaseBoundaryDoesNotImportActionPersistence() throws IOException {
        Path riskCase = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/riskcase");
        if (Files.isDirectory(riskCase)) {
            try (Stream<Path> paths = Files.walk(riskCase)) {
                for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(file))
                            .doesNotContain(
                                    "com.brokeros.risk.action.infrastructure",
                                    "action_record", "action_operation", "action_access_log");
                }
            }
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return Files.isDirectory(current.resolve("docs")) ? current : current.getParent();
    }
}
