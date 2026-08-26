package com.brokeros.risk.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class SecurityArchitectureTests {

    private static final List<String> FORBIDDEN_DOMAIN_APPLICATION_DEPENDENCIES = List.of(
            "org.springframework.security",
            "jakarta.servlet",
            "org.springframework.jdbc",
            "javax.sql",
            "SecurityContextHolder",
            "HttpServletRequest",
            "Jwt");

    @Test
    void domainAndApplicationStayIndependentFromFrameworkAndPersistence() throws IOException {
        Path securityRoot = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/security");
        List<Path> inspectedFiles;
        try (Stream<Path> paths = Files.walk(securityRoot)) {
            inspectedFiles = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/domain/")
                            || path.toString().contains("/application/"))
                    .toList();
        }

        assertThat(inspectedFiles).isNotEmpty();
        for (Path file : inspectedFiles) {
            String source = Files.readString(file);
            assertThat(source)
                    .as("framework isolation for %s", file)
                    .doesNotContain(FORBIDDEN_DOMAIN_APPLICATION_DEPENDENCIES);
        }
    }

    @Test
    void productionSecurityModuleHasNoCallerActorHeaderOrRoleAuthority() throws IOException {
        Path securityRoot = repositoryRoot()
                .resolve("backend/src/main/java/com/brokeros/risk/security");
        String source;
        try (Stream<Path> paths = Files.walk(securityRoot)) {
            source = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(SecurityArchitectureTests::read)
                    .reduce("", (left, right) -> left + "\n" + right);
        }

        assertThat(source)
                .doesNotContain(
                        "X-Actor-Id",
                        "X-User-Id",
                        "X-Username",
                        "hasRole(",
                        "ROLE_ADMIN",
                        "permitAllProvider",
                        "securityEnabled");
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect source " + path, exception);
        }
    }

    private static Path repositoryRoot() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(workingDirectory.resolve("docs"))) {
            return workingDirectory;
        }
        Path parent = workingDirectory.getParent();
        if (parent != null && Files.isDirectory(parent.resolve("docs"))) {
            return parent;
        }
        throw new IllegalStateException("Cannot locate repository root");
    }
}
