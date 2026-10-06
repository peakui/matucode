package com.peakui.auth.util;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test void newHashesUseRandomSaltAndVerify() {
        String password = "Valid密码123!";
        String first = PasswordUtil.encode(password), second = PasswordUtil.encode(password);
        assertTrue(first.startsWith("$2")); assertNotEquals(first, second);
        assertTrue(PasswordUtil.matches(password, first)); assertFalse(PasswordUtil.matches("wrong", first));
    }
    @Test void legacyAccountCanLoginAndUpgrade() throws Exception {
        String password = "legacy-secret";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8)));
        assertTrue(PasswordUtil.matches(password, hash)); assertTrue(PasswordUtil.canUpgrade(password, hash));
        assertFalse(PasswordUtil.matches("wrong", hash));
    }
    @Test void overlongPasswordsCannotSilentlyTruncate() throws Exception {
        String raw = "密".repeat(25);
        assertThrows(RuntimeException.class, () -> PasswordUtil.encode(raw));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        assertTrue(PasswordUtil.matches(raw, hash)); assertFalse(PasswordUtil.canUpgrade(raw, hash));
        assertFalse(PasswordUtil.matches(raw, PasswordUtil.encode("a".repeat(72))));
    }
    @Test void malformedBcryptAndNullUpgradeInputAreRejected() {
        assertFalse(PasswordUtil.matches("secret", "$2b$12$not-a-valid-bcrypt-hash"));
        assertFalse(PasswordUtil.matches("secret", "$2b$99$....................................................."));
        assertFalse(PasswordUtil.matches(null, "$2a$12$....................................................."));
        assertFalse(PasswordUtil.canUpgrade(null, "a".repeat(64)));
        assertFalse(PasswordUtil.isBcrypt("$2broken"));
    }

    @Test void uppercaseLegacyHashesRemainVerifiable() throws Exception {
        String password = "legacy-secret";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        assertTrue(PasswordUtil.matches(password, hash));
    }
}
