package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class TransactionAmountTest {

    @Test
    public void parsesPositiveAmountsToTwoDecimals() {
        assertEquals(new BigDecimal("10.00"), TransactionAmount.parse("10"));
        assertEquals(new BigDecimal("0.01"), TransactionAmount.parse(" 0.01 "));
        assertEquals(new BigDecimal("1000000.00"), TransactionAmount.parse("1000000"));
    }

    @Test
    public void rejectsMalformedNegativeZeroAndOversizedAmounts() {
        String[] rejected = {null, "", "   ", "-1000000", "-0.01", "0", "0.00", "1e6", "1E100000000",
                "NaN", "Infinity", "12.345", "1,000", "+5", "abc", "1000000.01", "99999999"};
        for (String raw : rejected) {
            try {
                TransactionAmount.parse(raw);
                fail("expected rejection of " + raw);
            } catch (InvalidTransactionException expected) {
            }
        }
    }

    @Test(expected = InvalidTransactionException.class)
    public void requireValidRejectsNegativeBigDecimal() {
        TransactionAmount.requireValid(new BigDecimal("-5"));
    }

    @Test(expected = InvalidTransactionException.class)
    public void requireValidRejectsNull() {
        TransactionAmount.requireValid(null);
    }
}
