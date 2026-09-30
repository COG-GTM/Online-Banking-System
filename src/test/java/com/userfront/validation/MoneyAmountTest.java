package com.userfront.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class MoneyAmountTest {

    @Test
    public void parsesPositiveAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("10.00"), MoneyAmount.parse("10"));
        assertEquals(new BigDecimal("10.50"), MoneyAmount.parse(" 10.5 "));
        assertEquals(new BigDecimal("0.01"), MoneyAmount.parse("0.01"));
        assertEquals(new BigDecimal("12.30"), MoneyAmount.parse("12.300"));
        assertEquals(MoneyAmount.MAX_AMOUNT, MoneyAmount.parse("999999999.99"));
    }

    @Test
    public void rejectsInvalidInput() {
        String[] invalid = {
                null, "", "   ", "-100", "-0.01", "0", "0.00", "+5", "10.001", "1E-600000000", "1E+9",
                "1e3", "NaN", "Infinity", "-Infinity", "abc", "1,000", "10.", ".5", "0x10",
                "1000000000", "999999999.991", "00000000000000001"
        };
        for (String value : invalid) {
            try {
                MoneyAmount.parse(value);
                fail("Expected rejection of " + value);
            } catch (InvalidAmountException expected) {
                // expected
            }
        }
    }

    @Test
    public void requireValidRejectsNonPositiveAndOverPreciseValues() {
        BigDecimal[] invalid = {
                null, new BigDecimal("-5"), BigDecimal.ZERO, new BigDecimal("0.001"),
                new BigDecimal("1E-600000000"), new BigDecimal("1E+600000000")
        };
        for (BigDecimal value : invalid) {
            try {
                MoneyAmount.requireValid(value);
                fail("Expected rejection of " + value);
            } catch (InvalidAmountException expected) {
                // expected
            }
        }
    }
}
