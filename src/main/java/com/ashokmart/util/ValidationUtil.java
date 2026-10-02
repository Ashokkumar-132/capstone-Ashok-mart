package com.ashokmart.util;

import java.math.BigDecimal;
import java.util.Set;

/** Shared boundary validation for request values and service inputs. */
public final class ValidationUtil {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    private ValidationUtil() { }

    public static String required(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + " is required and must be at most " + maxLength + " characters.");
        }
        return normalized;
    }

    public static long positiveId(String value, String label) {
        try { long parsed = Long.parseLong(value); if (parsed > 0) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("A valid " + label + " is required.");
    }

    public static int positiveQuantity(String value) {
        try { int parsed = Integer.parseInt(value); if (parsed > 0 && parsed <= 10000) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Quantity must be between 1 and 10,000.");
    }

    public static BigDecimal nonNegativePrice(String value) {
        try {
            BigDecimal parsed = new BigDecimal(value == null ? "" : value.trim());
            if (parsed.signum() >= 0 && parsed.scale() <= 2 && parsed.precision() <= 12) return parsed;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Price must be a non-negative amount with at most two decimals.");
    }

    public static int rating(String value) {
        try { int parsed = Integer.parseInt(value); if (parsed >= 1 && parsed <= 5) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Rating must be between 1 and 5.");
    }

    public static int page(String value) { return boundedInteger(value, DEFAULT_PAGE, 1, Integer.MAX_VALUE, "Page"); }
    public static int pageSize(String value) { return boundedInteger(value, DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE, "Page size"); }

    public static String status(String value, Set<String> allowed) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        if (!allowed.contains(normalized)) throw new IllegalArgumentException("Invalid status.");
        return normalized;
    }

    private static int boundedInteger(String value, int fallback, int minimum, int maximum, String label) {
        if (value == null || value.isBlank()) return fallback;
        try { int parsed = Integer.parseInt(value); if (parsed >= minimum && parsed <= maximum) return parsed; }
        catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + " must be between " + minimum + " and " + maximum + ".");
    }
}
