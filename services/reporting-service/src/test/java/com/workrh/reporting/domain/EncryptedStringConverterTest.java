package com.workrh.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @AfterEach
    void tearDown() {
        System.clearProperty("workrh.connector.secret-key");
    }

    @Test
    void encryptsAndDecryptsConnectorSecret() {
        System.setProperty("workrh.connector.secret-key", Base64.getEncoder().encodeToString(new byte[32]));

        String encrypted = converter.convertToDatabaseColumn("provider-token");

        assertThat(encrypted).startsWith("ENC[v1]:");
        assertThat(encrypted).doesNotContain("provider-token");
        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo("provider-token");
    }

    @Test
    void leavesLegacyPlaintextReadableForExistingRows() {
        assertThat(converter.convertToEntityAttribute("legacy-token")).isEqualTo("legacy-token");
    }
}
