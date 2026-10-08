package com.userfront.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class TransactionAmountTest {

    @Test
    public void parsesPlainAmountsToScaleTwo() {
        assertEquals(new BigDecimal("100.00"), TransactionAmount.parse("100"));
        assertEquals(new BigDecimal("12.50"), TransactionAmount.parse(" 12.5 "));
        assertEquals(new BigDecimal("0.01"), TransactionAmount.parse("0.01"));
        assertEquals(new BigDecimal("999999999999.99"), TransactionAmount.parse("999999999999.99"));
    }

    @Test
    public void rejectsHugeExponentsBeforeConstructingBigDecimal() {
        assertRejected("1E999999999");
        assertRejected("1e999999999");
        assertRejected("1E100000000");
        assertRejected("1E-100000000");
        assertRejected("1E-999999999");
        assertRejected("1E+2147483647");
        assertRejected("0.1E-600000000");
    }

    @Test
    public void rejectsMalformedNegativeZeroAndOversizedAmounts() {
        assertRejected(null);
        assertRejected("");
        assertRejected("   ");
        assertRejected("-5");
        assertRejected("+5");
        assertRejected("0");
        assertRejected("0.00");
        assertRejected("1.234");
        assertRejected("1,000");
        assertRejected("NaN");
        assertRejected("Infinity");
        assertRejected("1000000000000");
        assertRejected("12.");
        assertRejected(".5");
    }

    private static void assertRejected(String raw) {
        try {
            TransactionAmount.parse(raw);
            fail("Expected InvalidAmountException for " + raw);
        } catch (InvalidAmountException expected) {
        }
    }
}
