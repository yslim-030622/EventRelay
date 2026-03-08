package com.eventrelay.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private final SignatureVerifier signatureVerifier = new SignatureVerifier();

    @Test
    void verifyGitHubAcceptsValidSignature() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + hmacHex("secret", payload);

        assertThat(signatureVerifier.verify("github", payload, Map.of("x-hub-signature-256", signature), "secret"))
            .isTrue();
    }

    @Test
    void verifyGitHubRejectsInvalidSignature() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(signatureVerifier.verify("github", payload, Map.of("x-hub-signature-256", "sha256=invalid"), "secret"))
            .isFalse();
    }

    private String hmacHex(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload);
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
