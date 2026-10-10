package com.ashokmart.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/** Shared INR formatting for non-JSP views, logs, and future API responses. */
public final class CurrencyUtil {
    private static final Locale INDIA = Locale.forLanguageTag("en-IN");
    private static final Currency INR = Currency.getInstance("INR");

    private CurrencyUtil() {
    }

    public static String formatInr(BigDecimal amount) {
        BigDecimal value = (amount == null ? BigDecimal.ZERO : amount).setScale(2, RoundingMode.HALF_UP);
        String plain = value.abs().toPlainString();
        int decimalPoint = plain.indexOf('.');
        String integerPart = decimalPoint < 0 ? plain : plain.substring(0, decimalPoint);
        String fractionPart = decimalPoint < 0 ? "00" : plain.substring(decimalPoint + 1);
        String grouped = groupIndianInteger(integerPart);
        String sign = value.signum() < 0 ? "-" : "";
        return sign + currencySymbol() + grouped + "." + fractionPart;
    }

    private static String groupIndianInteger(String integerPart) {
        if (integerPart.length() <= 3) {
            return integerPart;
        }
        String lastThree = integerPart.substring(integerPart.length() - 3);
        String prefix = integerPart.substring(0, integerPart.length() - 3);
        StringBuilder grouped = new StringBuilder();
        while (prefix.length() > 2) {
            int start = prefix.length() - 2;
            grouped.insert(0, "," + prefix.substring(start));
            prefix = prefix.substring(0, start);
        }
        if (!prefix.isEmpty()) {
            grouped.insert(0, prefix);
        }
        return grouped + "," + lastThree;
    }

    private static String currencySymbol() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIA);
        formatter.setCurrency(INR);
        String symbol = formatter.getCurrency().getSymbol(INDIA);
        return symbol == null || symbol.isBlank() ? "₹" : symbol;
    }
}
