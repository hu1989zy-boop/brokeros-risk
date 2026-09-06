package com.brokeros.risk.tradingdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TradingDataBootstrapContractTests {

    @Test
    void ingestionBootstrapProvisionsOneServiceWithOnlyIngestCapability()
            throws IOException {
        Path bootstrap = repositoryRoot()
                .resolve("deploy/keycloak/q015-ingestion-bootstrap.json");
        JsonNode root = new ObjectMapper().readTree(Files.readString(bootstrap));

        assertThat(root.path("actors").size()).isEqualTo(1);
        JsonNode actor = root.path("actors").get(0);
        assertThat(actor.path("actorType").asText()).isEqualTo("SERVICE");
        assertThat(actor.path("principals").size()).isEqualTo(1);
        assertThat(actor.path("capabilities").size()).isEqualTo(1);
        assertThat(actor.path("capabilities").get(0).asText())
                .isEqualTo("trading-data:ingest");
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        return Files.isDirectory(current.resolve("docs")) ? current : current.getParent();
    }
}
