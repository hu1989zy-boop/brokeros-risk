package com.brokeros.risk.decision;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.brokeros.risk.decision.domain.DecisionProvenanceView;
import org.junit.jupiter.api.Test;

class DecisionArchitectureTests {

    private static final List<String> FORBIDDEN = List.of(
            "org.springframework", "jakarta.servlet", "javax.sql", "java.sql",
            "com.fasterxml.jackson", "org.apache.kafka", "org.springframework.data.redis",
            "mt4", "mt5");

    @Test
    void domainAndApplicationHaveNoFrameworkPersistenceOrVendorImports() throws IOException {
        Path root = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/decision");
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> files = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/domain/")
                            || path.toString().contains("/application/"))
                    .toList();
            assertThat(files).isNotEmpty();
            for (Path file : files) {
                assertThat(Files.readString(file)).as("isolation for %s", file)
                        .doesNotContain(FORBIDDEN);
            }
        }
    }

    @Test
    void narrowProvenanceContractCannotExposeConclusionText() {
        assertThat(Stream.of(DecisionProvenanceView.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("conclusionText", "conclusion", "reason");
    }

    @Test
    void moduleHasNoDeleteCorrectionSupersessionOrRawContentLogging() throws IOException {
        Path root = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/decision");
        String all;
        try (Stream<Path> paths = Files.walk(root)) {
            all = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(DecisionArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
        assertThat(all).doesNotContainIgnoringCase(
                "DELETE FROM decision_", "TRUNCATE decision_", "UPDATE decision_",
                "Correction", "Supersed", "permitAllProvider", "SYSTEM_ACTOR",
                "X-Actor", "logger.info", "logger.debug");
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
