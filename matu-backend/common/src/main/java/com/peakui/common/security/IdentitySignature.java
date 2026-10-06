package com.peakui.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/** 网关身份签名以及与同一身份、时间戳绑定的客户端 IP 签名。 */
public final class IdentitySignature {
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_ROLES = "X-User-Roles";
    public static final String HEADER_TIMESTAMP = "X-User-Timestamp";
    public static final String HEADER_SIGNATURE = "X-User-Signature";
    public static final String HEADER_CLIENT_IP = "X-Client-IP";
    public static final String HEADER_CLIENT_IP_SIGNATURE = "X-Client-IP-Signature";

    private static final String ALGORITHM = "HmacSHA256";

    private IdentitySignature() {
    }

    public static boolean isValidSecret(String secret) {
        return hasText(secret) && secret.length() >= 32;
    }

    public static String sign(String userId, String roles, long timestamp, String secret) {
        return encode(userId + "|" + normalizeRoles(roles) + "|" + timestamp, secret);
    }

    /** 协议明文固定为 userId|clientIp|timestamp；与身份签名共用时间戳和密钥。 */
    public static String signClientIp(String userId, String clientIp, long timestamp, String secret) {
        return encode(userId + "|" + clientIp + "|" + timestamp, secret);
    }

    public static boolean verify(String userId, String roles, String timestamp,
                                 String signature, String secret, long allowedSkewMillis) {
        Long parsed = validTimestamp(timestamp, allowedSkewMillis);
        return hasText(userId) && hasText(signature) && isValidSecret(secret) && parsed != null
                && constantTimeEquals(sign(userId, roles, parsed, secret), signature);
    }

    public static boolean verifyClientIp(String userId, String clientIp, String timestamp,
                                         String signature, String secret, long allowedSkewMillis) {
        Long parsed = validTimestamp(timestamp, allowedSkewMillis);
        return hasText(userId) && hasText(clientIp) && hasText(signature) && isValidSecret(secret)
                && parsed != null
                && constantTimeEquals(signClientIp(userId, clientIp, parsed, secret), signature);
    }

    private static Long validTimestamp(String timestamp, long allowedSkewMillis) {
        if (!hasText(timestamp) || allowedSkewMillis < 0) {
            return null;
        }
        try {
            long parsed = Long.parseLong(timestamp);
            // 非负 epoch 时间戳之间作较大值减较小值，避免减法及 Math.abs(Long.MIN_VALUE) 溢出。
            if (parsed < 0) {
                return null;
            }
            long now = System.currentTimeMillis();
            long difference = parsed >= now ? parsed - now : now - parsed;
            return difference <= allowedSkewMillis ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public static String normalizeRoles(String roles) {
        return roles == null ? "" : roles.trim();
    }

    private static String encode(String value, String secret) {
        if (!isValidSecret(secret)) {
            throw new IllegalArgumentException("身份签名密钥至少需要 32 个字符");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成身份签名", e);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
