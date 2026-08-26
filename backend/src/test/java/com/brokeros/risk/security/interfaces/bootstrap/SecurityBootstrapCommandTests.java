package com.brokeros.risk.security.interfaces.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.brokeros.risk.security.application.ProvisioningManifest;
import com.brokeros.risk.security.domain.ActorType;
import org.junit.jupiter.api.Test;

class SecurityBootstrapCommandTests {

    @Test
    void convertsBoundedManifestWithoutCredentials() {
        SecurityBootstrapCommand.BootstrapManifestInput input =
                new SecurityBootstrapCommand.BootstrapManifestInput(
                        "deployment",
                        "manifest-v1",
                        List.of(new SecurityBootstrapCommand.BootstrapActorInput(
                                "HUMAN",
                                List.of(new SecurityBootstrapCommand.BootstrapPrincipalInput(
                                        "https://issuer.test",
                                        "subject-1")),
                                List.of("security-test:read"))));

        ProvisioningManifest manifest = SecurityBootstrapCommand.toManifest(input);

        assertThat(manifest.actors()).hasSize(1);
        assertThat(manifest.actors().getFirst().actorType()).isEqualTo(ActorType.HUMAN);
        assertThat(manifest.actors().getFirst().capabilities())
                .extracting(capability -> capability.value())
                .containsExactly("security-test:read");
    }

    @Test
    void rejectsSystemAndUnknownActorTypes() {
        assertThatThrownBy(() -> SecurityBootstrapCommand.toManifest(inputFor("SYSTEM")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecurityBootstrapCommand.toManifest(inputFor("human")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static SecurityBootstrapCommand.BootstrapManifestInput inputFor(String actorType) {
        return new SecurityBootstrapCommand.BootstrapManifestInput(
                "deployment",
                "manifest-v1",
                List.of(new SecurityBootstrapCommand.BootstrapActorInput(
                        actorType,
                        List.of(new SecurityBootstrapCommand.BootstrapPrincipalInput(
                                "https://issuer.test",
                                "subject-1")),
                        List.of())));
    }
}
