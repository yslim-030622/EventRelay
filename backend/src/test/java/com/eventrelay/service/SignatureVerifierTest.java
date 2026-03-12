package com.eventrelay.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureVerifierTest {

    private final SignatureVerifier signatureVerifier = new SignatureVerifier();

    // --- GitHub tests ---

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

    @Test
    void verifyGitHubRejectsNullSignature() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(signatureVerifier.verifyGitHub(payload, null, "secret"))
            .isFalse();
    }

    @Test
    void verifyGitHubRejectsMissingPrefix() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signatureWithoutPrefix = hmacHex("secret", payload);

        assertThat(signatureVerifier.verifyGitHub(payload, signatureWithoutPrefix, "secret"))
            .isFalse();
    }

    @Test
    void verifyGitHubRejectsWrongSecret() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + hmacHex("wrong-secret", payload);

        assertThat(signatureVerifier.verifyGitHub(payload, signature, "correct-secret"))
            .isFalse();
    }

    // --- Stripe tests ---

    @Test
    void verifyStripeAcceptsValidSignature() {
        byte[] payload = "{\"type\":\"payment_intent.succeeded\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signedPayload = timestamp + "." + new String(payload, StandardCharsets.UTF_8);
        String sig = hmacHex("stripe-secret", signedPayload.getBytes(StandardCharsets.UTF_8));
        String header = "t=" + timestamp + ",v1=" + sig;

        assertThat(signatureVerifier.verifyStripe(payload, header, "stripe-secret"))
            .isTrue();
    }

    @Test
    void verifyStripeRejectsExpiredTimestamp() {
        byte[] payload = "{\"type\":\"payment_intent.succeeded\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond() - 400);
        String signedPayload = timestamp + "." + new String(payload, StandardCharsets.UTF_8);
        String sig = hmacHex("stripe-secret", signedPayload.getBytes(StandardCharsets.UTF_8));
        String header = "t=" + timestamp + ",v1=" + sig;

        assertThat(signatureVerifier.verifyStripe(payload, header, "stripe-secret"))
            .isFalse();
    }

    @Test
    void verifyStripeRejectsEmptyHeader() {
        byte[] payload = "{\"type\":\"payment_intent.succeeded\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(signatureVerifier.verifyStripe(payload, "", "stripe-secret"))
            .isFalse();
    }

    @Test
    void verifyStripeRejectsNullHeader() {
        byte[] payload = "{\"type\":\"payment_intent.succeeded\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(signatureVerifier.verifyStripe(payload, null, "stripe-secret"))
            .isFalse();
    }

    // --- Unknown source ---

    @Test
    void verifyUnknownSourcePassesThrough() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);

        assertThat(signatureVerifier.verify("unknown", payload, Map.of(), "any-secret"))
            .isTrue();
    }

    // --- Routing ---

    @Test
    void verifyDispatchesToGitHubForGithubSource() {
        byte[] payload = "{\"action\":\"opened\"}".getBytes(StandardCharsets.UTF_8);
        String validSignature = "sha256=" + hmacHex("secret", payload);

        assertThat(signatureVerifier.verify("github", payload, Map.of("x-hub-signature-256", validSignature), "secret"))
            .isTrue();
        assertThat(signatureVerifier.verify("github", payload, Map.of("x-hub-signature-256", "sha256=bad"), "secret"))
            .isFalse();
    }

    @Test
    void verifyDispatchesToStripeForStripeSource() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String sig = hmacHex("secret", (timestamp + "." + new String(payload, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
        String header = "t=" + timestamp + ",v1=" + sig;

        assertThat(signatureVerifier.verify("stripe", payload, Map.of("stripe-signature", header), "secret"))
            .isTrue();
        assertThat(signatureVerifier.verify("stripe", payload, Map.of("stripe-signature", "bad"), "secret"))
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
