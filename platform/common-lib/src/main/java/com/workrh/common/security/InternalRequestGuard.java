package com.workrh.common.security;

import com.workrh.common.web.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class InternalRequestGuard {

    private InternalRequestGuard() {
    }

    public static void requireValidKey(String configuredKey, String providedKey, String keyName) {
        if (isBlank(configuredKey)) {
            throw new UnauthorizedException(keyName + " is not configured");
        }
        if (isBlank(providedKey) || !constantTimeEquals(configuredKey, providedKey)) {
            throw new UnauthorizedException("Invalid " + keyName);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
