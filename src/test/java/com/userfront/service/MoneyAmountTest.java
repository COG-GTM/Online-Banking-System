package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class MoneyAmountTest {

    @Test
    public void parsesPositiveAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("12.50"), MoneyAmount.parse("12.5"));
        assertEquals(new BigDecimal("0.01"), MoneyAmount.parse(" 0.01 "));
        assertEquals(new BigDecimal("1000000.00"), MoneyAmount.parse("1000000"));
    }

    @Test
    public void rejectsNonPositiveMalformedAndUnboundedAmounts() {
        String[] rejected = {null, "", "  ", "0", "0.00", "-500000", "-0.01", "+5", "NaN", "Infinity", "-Infinity",
                "1e3", "0x10", "1,000", "12.345", "1000000.01", "99999999", "abc"};
        for (String raw : rejected) {
            try {
                MoneyAmount.parse(raw);
                fail("expected rejection of " + raw);
            } catch (InvalidTransactionException expected) {
                // expected
            }
        }
    }

    @Test(expected = InvalidTransactionException.class)
    public void requireValidRejectsNegativeBigDecimal() {
        MoneyAmount.requireValid(new BigDecimal("-1"));
    }

    @Test(expected = InvalidTransactionException.class)
    public void requireValidRejectsExcessScale() {
        MoneyAmount.requireValid(new BigDecimal("1.001"));
    }
}
