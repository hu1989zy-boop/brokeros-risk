package com.brokeros.risk.tradingdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import com.brokeros.risk.tradingdata.interfaces.rest.TradingDataIngestionController;

class TradingDataArchitectureTests {

    @Test
    void moduleHasNoGatewayNativeSdkOrUnsafeLoggingSurface() throws IOException {
        Path module = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/tradingdata");
        try (Stream<Path> paths = Files.walk(module)) {
            List<Path> files = paths.toList();
            assertThat(files.stream().filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString()))
                    .doesNotContain("gateway");
            String source = files.stream()
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(TradingDataArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(source).doesNotContainIgnoringCase(
                    "Manager API", "native adapter", "native field",
                    "order ticket", "deal ticket", "position ticket",
                    "logger.info", "logger.debug", "payload.toString");
        }
    }

    @Test
    void legacyHttpTestAidIsAbsentByDefault() {
        new WebApplicationContextRunner()
                .withUserConfiguration(TradingDataIngestionController.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TradingDataIngestionController.class);
                });
    }

    @Test
    void canonicalParsingDoesNotLeakIntoPersistenceOrReliability() throws IOException {
        Path module = repositoryRoot().resolve("backend/src/main/java/com/brokeros/risk/tradingdata");
        for (String folder : List.of("application", "infrastructure/persistence")) {
            try (Stream<Path> paths = Files.walk(module.resolve(folder))) {
                String source = paths.filter(path -> path.toString().endsWith(".java"))
                        .map(TradingDataArchitectureTests::read).reduce("", String::concat);
                assertThat(source).doesNotContain("ObjectMapper", "JsonNode", "CanonicalTradingDataEvent");
            }
        }
    }

    @Test
    void storeIsolatedToNewTablesAndPublisherUsesApprovedTopic() throws IOException {
        Path module = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/tradingdata");
        String source;
        try (Stream<Path> paths = Files.walk(module)) {
            source = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(TradingDataArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
        assertThat(source)
                .contains("trading_data_event", "trading_data_ingestion_gap",
                        "trading-data.canonical")
                .doesNotContain(
                        "evidence_record", "decision_record", "action_record",
                        "action_outcome_record", "risk_case", "trading_account_reference");
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
