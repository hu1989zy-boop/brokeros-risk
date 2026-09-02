package com.brokeros.risk.riskcase;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.brokeros.risk.riskcase.domain.RiskCase;
import org.junit.jupiter.api.Test;

class RiskCaseArchitectureTests {

    private static final Path MAIN = Path.of("src/main/java/com/brokeros/risk");

    @Test
    void domainIsFrameworkPersistenceAndVendorIndependent() throws IOException {
        String source = sources(MAIN.resolve("riskcase/domain"));
        assertThat(source)
                .doesNotContain("org.springframework")
                .doesNotContain("java.sql")
                .doesNotContain("JdbcTemplate")
                .doesNotContain("jakarta.persistence")
                .doesNotContain("MT4")
                .doesNotContain("MT5")
                .doesNotContain("CRM");
    }

    @Test
    void applicationOwnsTransactionsButHasNoJdbcOrVendorAccess() throws IOException {
        String source = sources(MAIN.resolve("riskcase/application"));
        assertThat(source)
                .doesNotContain("JdbcTemplate")
                .doesNotContain("java.sql")
                .doesNotContain("infrastructure.persistence")
                .doesNotContain("MT4")
                .doesNotContain("KafkaTemplate")
                .doesNotContain("RedisTemplate");
        assertThat(source).contains("TransactionTemplate");
    }

    @Test
    void aggregateExposesNamedOperationsAndNoGenericStatusMutation() {
        List<String> methods = List.of(RiskCase.class.getDeclaredMethods()).stream()
                .map(method -> method.getName())
                .toList();
        assertThat(methods).contains(
                "openManual", "openDecisionDriven", "assign", "unassign",
                "beginReview", "associateEvidence", "changeEvidenceDisposition",
                "associateDecision", "selectCurrentDecision", "associateAction",
                "recordActionOutcomeReference", "markActionRequired", "returnToReview",
                "changePriority", "addInvestigationNote", "correctInvestigationNote",
                "resolve", "close", "cancel", "resumeResolvedCase",
                "reopenClosedCase");
        assertThat(methods).doesNotContain("setStatus", "delete", "executeAction");
    }

    @Test
    void riskCaseNeverImportsUpstreamPersistenceOrExternalExecution() throws IOException {
        String source = sources(MAIN.resolve("riskcase"));
        assertThat(source)
                .doesNotContain("tradingaccount.infrastructure.persistence")
                .doesNotContain("evidence.infrastructure.persistence")
                .doesNotContain("decision.infrastructure.persistence")
                .doesNotContain("action.infrastructure.persistence")
                .doesNotContain("actionoutcome.infrastructure.persistence")
                .doesNotContain("ManagerApi")
                .doesNotContain("KafkaTemplate")
                .doesNotContain("RedisTemplate")
                .doesNotContain("@DeleteMapping")
                .doesNotContain("REQUIRES_NEW");
    }

    @Test
    void auditBoundaryDoesNotDependOnRiskCaseDomain() throws IOException {
        String source = sources(MAIN.resolve("audit"));
        assertThat(source).doesNotContain("com.brokeros.risk.riskcase");
    }

    private String sources(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(path -> path.toString().endsWith(".java"))
                    .sorted()
                    .map(this::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
