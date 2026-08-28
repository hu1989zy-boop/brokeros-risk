package com.brokeros.risk.tradingaccount.interfaces.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.brokeros.risk.api.ResultCode;
import com.brokeros.risk.exception.BusinessException;
import com.brokeros.risk.tradingaccount.domain.AuthorityOperationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TradingAccountAuthorityBootstrapCommandTests {

    @Test
    void strictMapperRejectsUnknownDuplicateAndTrailingContent() throws Exception {
        ObjectMapper mapper = TradingAccountAuthorityBootstrapCommand.strictMapper(new ObjectMapper());
        String valid = validJson();
        assertThat(mapper.readValue(valid,
                TradingAccountAuthorityBootstrapCommand.BootstrapManifestInput.class)).isNotNull();
        assertThatThrownBy(() -> mapper.readValue(
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"unknown\":true"),
                TradingAccountAuthorityBootstrapCommand.BootstrapManifestInput.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> mapper.readValue(
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"),
                TradingAccountAuthorityBootstrapCommand.BootstrapManifestInput.class))
                .isInstanceOf(JsonProcessingException.class);
        assertThatThrownBy(() -> mapper.readValue(valid + " {}",
                TradingAccountAuthorityBootstrapCommand.BootstrapManifestInput.class))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void typedMappingRejectsForbiddenFieldsAndReturnsBoundedResultCode() throws Exception {
        ObjectMapper mapper = TradingAccountAuthorityBootstrapCommand.strictMapper(new ObjectMapper());
        var input = mapper.readValue(validJson().replace(
                        "\"authorityScopeRef\":null",
                        "\"authorityScopeRef\":\"aas-00000000-0000-4000-8000-000000000001\""),
                TradingAccountAuthorityBootstrapCommand.BootstrapManifestInput.class);

        assertThatThrownBy(() -> TradingAccountAuthorityBootstrapCommand.toRequest(
                input, AuthorityOperationType.REGISTER_AUTHORITY_SCOPE))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getResultCode())
                        .isEqualTo(ResultCode.TRADING_ACCOUNT_MANIFEST_INVALID))
                .hasMessageNotContaining("approval-secret");
    }

    @Test
    void invalidInvocationDoesNotStartApplicationAndUsesExitTwo() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int exit = TradingAccountAuthorityBootstrapCommand.run(
                new String[0], new PrintStream(bytes));
        assertThat(exit).isEqualTo(2);
        assertThat(bytes.toString())
                .contains("TRADING_ACCOUNT_MANIFEST_INVALID")
                .doesNotContain("Exception", "stack");
    }

    private String validJson() {
        return """
                {
                  "schemaVersion":1,
                  "operationId":"00000000-0000-4000-8000-000000000001",
                  "operation":"REGISTER_AUTHORITY_SCOPE",
                  "authorityScopeRef":null,
                  "tradingAccountRef":null,
                  "sourceNamespace":null,
                  "externalAccountKey":null,
                  "expectedVersion":null,
                  "attestation":{"source":"broker-record","reference":"approval-secret"},
                  "reason":"Initial controlled registration",
                  "changeRef":"change-1"
                }
                """;
    }
}
