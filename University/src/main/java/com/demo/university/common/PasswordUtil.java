package com.demo.university.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 加盐 SHA-256 口令工具。
 * 数据库中只存 salt 和 hash，永不存明文；校验使用常量时间比较，避免时序攻击。
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;

    private PasswordUtil() {
    }

    /** 生成 32 位十六进制盐值 */
    public static String randomSalt() {
        byte[] bytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** hash = SHA-256(salt + password) */
    public static String hash(String rawPassword, String salt) {
        if (rawPassword == null || salt == null) {
            throw new IllegalArgumentException("密码与盐值不能为空");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt.getBytes(StandardCharsets.UTF_8));
            digest.update(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }

    /** 校验明文口令是否与库中记录匹配 */
    public static boolean matches(String rawPassword, String salt, String storedHash) {
        if (rawPassword == null || salt == null || storedHash == null) {
            return false;
        }
        String actual = hash(rawPassword, salt);
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
