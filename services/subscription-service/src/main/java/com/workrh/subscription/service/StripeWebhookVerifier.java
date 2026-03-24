package com.workrh.subscription.service;

import com.workrh.common.web.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class StripeWebhookVerifier {

    public void verify(String payload, String stripeSignature, String endpointSecret) {
        if (stripeSignature == null || stripeSignature.isBlank() || endpointSecret == null || endpointSecret.isBlank()) {
            throw new UnauthorizedException("Missing Stripe webhook signature");
        }

        String timestamp = null;
        String signature = null;
        for (String part : stripeSignature.split(",")) {
            String[] split = part.split("=", 2);
            if (split.length != 2) {
                continue;
            }
            if ("t".equals(split[0])) {
                timestamp = split[1];
            } else if ("v1".equals(split[0])) {
                signature = split[1];
            }
        }

        if (timestamp == null || signature == null) {
            throw new UnauthorizedException("Invalid Stripe signature header");
        }

        String signedPayload = timestamp + "." + payload;
        String expected = hmacSha256(endpointSecret, signedPayload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Stripe signature verification failed");
        }
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            Arrays.stream(toUnsigned(digest)).forEach(value -> builder.append(String.format("%02x", value)));
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify Stripe signature", exception);
        }
    }

    private int[] toUnsigned(byte[] values) {
        int[] result = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            result[index] = values[index] & 0xff;
        }
        return result;
    }
}
