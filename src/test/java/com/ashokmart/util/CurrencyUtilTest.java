package com.ashokmart.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrencyUtilTest {
    @Test
    void formatsIndianRupeesWithIndianGroupingAndTwoDecimals() {
        String formatted = CurrencyUtil.formatInr(new BigDecimal("125000.00"));
        assertTrue(formatted.contains("₹"));
        assertTrue(formatted.contains("1,25,000.00"), formatted);
    }

    @Test
    void formatsNullAsZeroRupees() {
        assertEquals("₹0.00", CurrencyUtil.formatInr(null));
    }
}
