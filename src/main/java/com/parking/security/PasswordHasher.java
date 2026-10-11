package com.parking.security;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {

    private static final int ITERATIONS = 600_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PasswordHasher() {
    }

    public static String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password cannot be empty.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] hash = deriveHash(password.toCharArray(), salt);

        return ITERATIONS + ":"
                + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(
            String password, String storedHash) {

        if (password == null || storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split(":");

            if (parts.length != 3) {
                return false;
            }

            int iterations = Integer.parseInt(parts[0]);

            if (iterations < 100_000 || iterations > 2_000_000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expectedHash =
                    Base64.getDecoder().decode(parts[2]);

            if (salt.length != SALT_LENGTH
                    || expectedHash.length != KEY_LENGTH / 8) {
                return false;
            }

            byte[] actualHash =
                    deriveHash(password.toCharArray(), salt, iterations);

            return java.security.MessageDigest.isEqual(
                    expectedHash, actualHash);

        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static byte[] deriveHash(char[] password, byte[] salt) {
        return deriveHash(password, salt, ITERATIONS);
    }

    private static byte[] deriveHash(
            char[] password, byte[] salt, int iterations) {

        PBEKeySpec spec = new PBEKeySpec(
                password, salt, iterations, KEY_LENGTH);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Could not hash password.", exception);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(password, '\0');
        }
    }
}