package com.brokeros.risk.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.StandardEnvironment;

class ConfigurationContractIntegrationTests {

    private static final Pattern PLACEHOLDER_ALIAS =
            Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)(?::[^}]*)?}");

    private static final Pattern KUBERNETES_ENVIRONMENT_NAME =
            Pattern.compile("(?m)^\\s*(?:-\\s+name:\\s+)?([A-Z][A-Z0-9_]+)(?::|\\s*$)");

    private static final List<String> CATALOG_COLUMNS = List.of(
            "Owner",
            "Canonical Property",
            "Environment Alias",
            "Type",
            "Default",
            "Required",
            "Profile",
            "Sensitivity",
            "Validation",
            "Source",
            "Restart Required",
            "Compatibility");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withInitializer(context -> {
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            });

    @Test
    void loadsTestAndProductionProfileOverrides() {
        String syntheticSensitiveValue = UUID.randomUUID().toString();

        contextRunner
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .contains("brokeros_risk_test");
                    assertThat(context.getEnvironment().getProperty("spring.kafka.consumer.group-id"))
                            .isEqualTo("brokeros-risk-test");
                    assertThat(context.getEnvironment().getProperty("logging.level.com.brokeros.risk"))
                            .isEqualTo("DEBUG");
                });

        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "DB_USERNAME=configuration-contract-user",
                        "DB_PASSWORD=" + syntheticSensitiveValue)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty(
                                    "spring.datasource.hikari.maximum-pool-size", Integer.class))
                            .isEqualTo(20);
                    assertThat(context.getEnvironment().getProperty("logging.level.com.brokeros.risk"))
                            .isEqualTo("INFO");
                });
    }

    @Test
    void rejectsMissingRequiredProductionProperty() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getEnvironment()
                                    .getRequiredProperty("spring.datasource.password"))
                            .isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("DB_PASSWORD");
                });
    }

    @Test
    void rejectsInvalidFrameworkPropertyType() {
        contextRunner
                .withPropertyValues("DB_POOL_MAX_SIZE=not-an-integer")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThatThrownBy(() -> context.getEnvironment().getRequiredProperty(
                                    "spring.datasource.hikari.maximum-pool-size", Integer.class))
                            .isInstanceOf(ConversionFailedException.class);
                });
    }

    @Test
    void environmentAliasOverridesPackagedDefault() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test", "DB_POOL_MAX_SIZE=37")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty(
                                    "spring.datasource.hikari.maximum-pool-size", Integer.class))
                            .isEqualTo(37);
                });
    }

    @Test
    void invalidNonSecretDiagnosticDoesNotContainSuppliedSensitiveValue() {
        String sensitiveValue = "configuration-contract-" + UUID.randomUUID();

        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "DB_USERNAME=configuration-contract-user",
                        "DB_PASSWORD=" + sensitiveValue,
                        "DB_POOL_MAX_SIZE=not-an-integer")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    Throwable failure = catchThrowable(() -> context.getEnvironment().getRequiredProperty(
                            "spring.datasource.hikari.maximum-pool-size", Integer.class));

                    assertThat(failure)
                            .isInstanceOf(ConversionFailedException.class)
                            .hasMessageNotContaining(sensitiveValue);
                });
    }

    @Test
    void actuatorConfigurationEndpointsRemainUnexposed() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
                    .isEqualTo("health,info")
                    .doesNotContain("env", "configprops");
        });
    }

    @Test
    void catalogDocumentsConfigurationContractAndEveryDeploymentAlias() throws IOException {
        Path repositoryRoot = repositoryRoot();
        String catalog = Files.readString(repositoryRoot.resolve("docs/configuration/README.md"));

        assertThat(catalog).contains("## Validation Strategy", "## Secret Convention");
        CATALOG_COLUMNS.forEach(column -> assertThat(catalog).contains("| " + column + " "));

        Set<String> deploymentAliases = deploymentAliases(repositoryRoot);
        assertThat(deploymentAliases).isNotEmpty();
        deploymentAliases.forEach(alias -> assertThat(catalog).contains("`" + alias + "`"));
    }

    private static Set<String> deploymentAliases(Path repositoryRoot) throws IOException {
        Set<String> aliases = new LinkedHashSet<>();

        List<Path> placeholderSources = List.of(
                repositoryRoot.resolve("backend/src/main/resources/application.yml"),
                repositoryRoot.resolve("backend/src/main/resources/application-test.yml"),
                repositoryRoot.resolve("backend/src/main/resources/application-prod.yml"),
                repositoryRoot.resolve("docker-compose.yml"));

        for (Path source : placeholderSources) {
            collectMatches(Files.readString(source), PLACEHOLDER_ALIAS, aliases);
        }

        Path kubernetesDirectory = repositoryRoot.resolve("deploy/kubernetes");
        try (Stream<Path> paths = Files.walk(kubernetesDirectory)) {
            for (Path source : paths.filter(path -> path.toString().endsWith(".yaml")).toList()) {
                collectMatches(Files.readString(source), KUBERNETES_ENVIRONMENT_NAME, aliases);
            }
        }

        return aliases;
    }

    private static void collectMatches(String content, Pattern pattern, Set<String> aliases) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            aliases.add(matcher.group(1));
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

        throw new IllegalStateException("Cannot locate repository root for configuration contract tests");
    }
}
