package com.eventrelay.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

@Service
public class SignatureVerifier {

    public boolean verify(String sourceName, byte[] payload, Map<String, Object> headers, String secret) {
        if (secret == null || secret.isBlank()) {
            return true;
        }

        return switch (sourceName) {
            case "github" -> verifyGitHub(payload, getHeader(headers, "x-hub-signature-256"), secret);
            case "stripe" -> verifyStripe(payload, getHeader(headers, "stripe-signature"), secret);
            default -> true;
        };
    }

    public boolean verifyGitHub(byte[] payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        String expected = "sha256=" + hmacSha256Hex(secret, payload);
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    public boolean verifyStripe(byte[] payload, String signature, String secret) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        String timestamp = null;
        String v1Sig = null;
        for (String part : signature.split(",")) {
            if (part.startsWith("t=")) timestamp = part.substring(2);
            else if (part.startsWith("v1=")) v1Sig = part.substring(3);
        }
        if (timestamp == null || v1Sig == null) {
            return false;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return false;
        }

        long nowEpoch = Instant.now().getEpochSecond();
        if (Math.abs(nowEpoch - ts) > 300) {
            return false;
        }

        String signedPayload = timestamp + "." + new String(payload, StandardCharsets.UTF_8);
        String expected = hmacSha256Hex(secret, signedPayload.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            v1Sig.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String getHeader(Map<String, Object> headers, String key) {
        Object value = headers.get(key);
        return value == null ? null : value.toString();
    }

    private String hmacSha256Hex(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload);
            StringBuilder builder = new StringBuilder();
            for (byte currentByte : hash) {
                builder.append(String.format("%02x", currentByte));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate HMAC signature", exception);
        }
    }
}
