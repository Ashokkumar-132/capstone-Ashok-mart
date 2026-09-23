package com.ashokmart.util;

import org.mindrot.jbcrypt.BCrypt;

/** BCrypt-only password hashing and verification. */
public final class PasswordUtil {
    private static final int WORK_FACTOR = 12;

    private PasswordUtil() {
    }

    public static String hash(String plaintextPassword) {
        if (plaintextPassword == null || plaintextPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        return BCrypt.hashpw(plaintextPassword, BCrypt.gensalt(WORK_FACTOR));
    }

    public static boolean verify(String plaintextPassword, String passwordHash) {
        if (plaintextPassword == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintextPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
