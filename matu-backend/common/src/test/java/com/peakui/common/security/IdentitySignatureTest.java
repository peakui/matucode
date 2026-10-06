package com.peakui.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentitySignatureTest {
    private static final String SECRET = "01234567890123456789012345678901";

    @Test
    void signsAndVerifiesClientIpWithSharedTimestamp() {
        long timestamp = System.currentTimeMillis();
        String identity = IdentitySignature.sign("42", "admin,user", timestamp, SECRET);
        String ip = IdentitySignature.signClientIp("42", "203.0.113.9", timestamp, SECRET);

        assertTrue(IdentitySignature.verify("42", "admin,user", Long.toString(timestamp), identity,
                SECRET, 1_000));
        assertTrue(IdentitySignature.verifyClientIp("42", "203.0.113.9", Long.toString(timestamp), ip,
                SECRET, 1_000));
        assertFalse(IdentitySignature.verifyClientIp("43", "203.0.113.9", Long.toString(timestamp), ip,
                SECRET, 1_000));
    }

    @Test
    void rejectsShortSecretAndTimestampOverflow() {
        assertFalse(IdentitySignature.isValidSecret("short"));
        assertThrows(IllegalArgumentException.class, () -> IdentitySignature.sign("0", "", 1, "short"));
        assertFalse(IdentitySignature.verify("0", "", Long.toString(Long.MIN_VALUE), "bad",
                SECRET, Long.MAX_VALUE));
        assertFalse(IdentitySignature.verify("0", "", Long.toString(Long.MAX_VALUE), "bad",
                SECRET, Long.MAX_VALUE));
    }
}
