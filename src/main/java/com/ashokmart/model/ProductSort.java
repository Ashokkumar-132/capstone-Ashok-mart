package com.ashokmart.model;

import java.util.Locale;

public enum ProductSort {
    NEWEST,
    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC,
    NAME_DESC;

    /** Converts an external query value through a fixed whitelist. */
    public static ProductSort fromExternal(String value) {
        if (value == null) {
            return NEWEST;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "newest" -> NEWEST;
            case "price_asc" -> PRICE_ASC;
            case "price_desc" -> PRICE_DESC;
            case "name_asc" -> NAME_ASC;
            case "name_desc" -> NAME_DESC;
            default -> NEWEST;
        };
    }
}
