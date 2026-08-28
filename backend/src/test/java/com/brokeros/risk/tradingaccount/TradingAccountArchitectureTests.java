package com.brokeros.risk.tradingaccount;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TradingAccountArchitectureTests {

    private static final List<String> FORBIDDEN = List.of(
            "org.springframework", "jakarta.servlet", "javax.sql", "com.fasterxml.jackson",
            "org.apache.kafka", "org.springframework.data.redis", "mt4", "mt5");

    @Test
    void domainAndApplicationHaveNoFrameworkPersistenceOrVendorImports() throws IOException {
        Path root = repositoryRoot().resolve(
                "backend/src/main/java/com/brokeros/risk/tradingaccount");
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
    void q010HasNoRestControllerDeletePortOrBypassVocabulary() throws IOException {
        Path root = repositoryRoot().resolve(
                "backend/src/main/java/com/brokeros/risk/tradingaccount");
        String all;
        try (Stream<Path> paths = Files.walk(root)) {
            all = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(TradingAccountArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }
        assertThat(all).doesNotContain(
                "@RestController", "@RequestMapping", "deleteAccount", "deleteScope",
                "X-Actor", "ROLE_ADMIN", "permitAllProvider", "SYSTEM_ACTOR");
    }

    private static String read(Path path) {
        try { return Files.readString(path); }
        catch (IOException exception) { throw new IllegalStateException(exception); }
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return Files.isDirectory(current.resolve("docs")) ? current : current.getParent();
    }
}
