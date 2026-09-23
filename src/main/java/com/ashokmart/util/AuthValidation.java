package com.ashokmart.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AuthValidation {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private AuthValidation() {
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static String validateName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name is required and must be at most 120 characters");
        }
        return normalized;
    }

    public static String validateEmail(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty() || normalized.length() > 255 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        return normalized;
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }
}
