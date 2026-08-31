package com.brokeros.risk.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.brokeros.risk.evidence.domain.EvidenceProvenanceView;
import com.brokeros.risk.evidence.application.EvidenceCorrectionService;
import org.junit.jupiter.api.Test;

class EvidenceArchitectureTests {

    private static final List<String> FORBIDDEN = List.of(
            "org.springframework", "jakarta.servlet", "javax.sql", "java.sql",
            "com.fasterxml.jackson", "org.apache.kafka", "org.springframework.data.redis",
            "mt4", "mt5");

    @Test
    void domainAndApplicationHaveNoFrameworkPersistenceOrVendorImports() throws IOException {
        Path root = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/evidence");
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
    void narrowProvenanceContractCannotExposeSensitiveContent() {
        assertThat(Stream.of(EvidenceProvenanceView.class.getRecordComponents())
                .map(RecordComponent::getName))
                .doesNotContain("observationText", "correctionReason", "reason");
        assertThat(Stream.of(EvidenceCorrectionService.class.getDeclaredFields())
                .map(field -> field.getType().getName()))
                .noneMatch(type -> type.contains("tradingaccount"));
    }

    @Test
    void moduleHasNoDeleteSqlBypassProviderOrRawContentLogging() throws IOException {
        Path root = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/evidence");
        String all;
        try (Stream<Path> paths = Files.walk(root)) {
            all = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(EvidenceArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
        assertThat(all).doesNotContainIgnoringCase(
                "DELETE FROM evidence_", "TRUNCATE evidence_", "permitAllProvider",
                "SYSTEM_ACTOR", "X-Actor", "logger.info", "logger.debug");
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
