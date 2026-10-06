package com.peakui.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具。
 *
 * 新密码使用 BCrypt；SHA-256 仅用于验证存量账号，成功后由认证服务升级。
 */
public final class PasswordUtil {
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);
    private static final String BCRYPT_PATTERN = "^\\$2[ayb]\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    private PasswordUtil() {
    }

    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new com.peakui.auth.exception.AuthException("密码的 UTF-8 长度不能超过72字节");
        }
        return BCRYPT.encode(rawPassword);
    }

    private static String legacyEncode(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte aByte : bytes) {
                builder.append(String.format("%02x", aByte));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) return false;
        if (isLegacy(encodedPassword)) {
            return MessageDigest.isEqual(legacyEncode(rawPassword).getBytes(StandardCharsets.US_ASCII),
                    encodedPassword.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72 || !isBcrypt(encodedPassword)) return false;
        try {
            return BCRYPT.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isLegacy(String hash) { return hash != null && hash.matches("[a-fA-F0-9]{64}"); }

    public static boolean isBcrypt(String hash) {
        return hash != null && hash.matches(BCRYPT_PATTERN);
    }

    public static boolean canUpgrade(String rawPassword, String hash) {
        return rawPassword != null && isLegacy(hash) && rawPassword.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
