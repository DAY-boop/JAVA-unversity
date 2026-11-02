package com.demo.university.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {

    @Test
    void hashIsDeterministicForSameSalt() {
        String salt = PasswordUtil.randomSalt();
        assertEquals(PasswordUtil.hash("admin123", salt), PasswordUtil.hash("admin123", salt));
    }

    @Test
    void differentSaltProducesDifferentHash() {
        String hash1 = PasswordUtil.hash("admin123", PasswordUtil.randomSalt());
        String hash2 = PasswordUtil.hash("admin123", PasswordUtil.randomSalt());
        assertNotEquals(hash1, hash2);
    }

    @Test
    void matchesWorks() {
        String salt = PasswordUtil.randomSalt();
        String hash = PasswordUtil.hash("admin123", salt);
        assertTrue(PasswordUtil.matches("admin123", salt, hash));
        assertFalse(PasswordUtil.matches("admin124", salt, hash));
        assertFalse(PasswordUtil.matches(null, salt, hash));
    }

    @Test
    void saltIs32HexChars() {
        assertEquals(32, PasswordUtil.randomSalt().length());
    }
}
