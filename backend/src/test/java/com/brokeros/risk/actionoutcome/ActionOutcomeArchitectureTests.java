package com.brokeros.risk.actionoutcome;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.brokeros.risk.actionoutcome.domain.ActionOutcomeProvenanceView;
import com.brokeros.risk.actionoutcome.domain.ActionOutcomeRecord;
import org.junit.jupiter.api.Test;

class ActionOutcomeArchitectureTests {

    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "org.springframework", "jakarta.servlet", "javax.sql", "java.sql",
            "com.fasterxml.jackson", "org.apache.kafka", "org.springframework.data.redis",
            "mt4", "mt5");

    @Test
    void domainAndApplicationHaveNoFrameworkPersistenceOrVendorImports()
            throws IOException {
        Path root = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/actionoutcome");
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
    void narrowProvenanceContractCannotExposeOutcomeText() {
        assertThat(Stream.of(ActionOutcomeProvenanceView.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain(
                        "outcomeText", "text", "content", "result", "classification");
    }

    @Test
    void recordHasNoLifecycleOrResultTaxonomy() {
        assertThat(Stream.of(ActionOutcomeRecord.class.getRecordComponents())
                .map(RecordComponent::getName))
                .containsExactly(
                        "actionOutcomeRef", "actionRef", "outcomeText", "source",
                        "recordedByActorRef", "recordedAt")
                .doesNotContain("status", "result", "classification");
    }

    @Test
    void moduleHasNoMutationDeleteExternalExecutionOrRawLoggingBehavior()
            throws IOException {
        Path root = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/actionoutcome");
        String all;
        try (Stream<Path> paths = Files.walk(root)) {
            all = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(ActionOutcomeArchitectureTests::read)
                    .reduce("", (left, right) ->
                            left + System.lineSeparator() + right);
        }
        assertThat(all).doesNotContainIgnoringCase(
                "DELETE FROM action_outcome_", "TRUNCATE action_outcome_",
                "UPDATE action_outcome_", "ActionOutcomeStatus",
                "Correction", "Withdrawal", "ExecutionAttempt",
                "AccountControl", "ManagerApi", "Succeeded", "PartialResult",
                "RiskCase", "leverage", "mt4", "mt5",
                "permitAllProvider", "SYSTEM_ACTOR", "X-Actor",
                "logger.info", "logger.debug");
    }

    @Test
    void futureRiskCaseBoundaryDoesNotImportActionOutcomePersistence()
            throws IOException {
        Path riskCase = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/riskcase");
        if (Files.isDirectory(riskCase)) {
            try (Stream<Path> paths = Files.walk(riskCase)) {
                for (Path file : paths
                        .filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertThat(Files.readString(file))
                            .doesNotContain(
                                    "com.brokeros.risk.actionoutcome.infrastructure",
                                    "action_outcome_record",
                                    "action_outcome_operation",
                                    "action_outcome_access_log");
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
