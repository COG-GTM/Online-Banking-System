package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

public class TransactionAmountsTest {

    @Test
    public void parsesPositiveAmountWithUpToTwoDecimals() {
        assertEquals(new BigDecimal("12.50"), TransactionAmounts.parse(" 12.50 "));
        assertEquals(new BigDecimal("100"), TransactionAmounts.parse("100"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeAmount() {
        TransactionAmounts.parse("-50");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroAmount() {
        TransactionAmounts.parse("0.00");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonNumericAmount() {
        TransactionAmounts.parse("NaN");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankAmount() {
        TransactionAmounts.parse("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsFractionalCents() {
        TransactionAmounts.parse("10.001");
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsAmountAboveMaximum() {
        TransactionAmounts.parse("1000000.01");
    }

    @Test
    public void coversOnlyWhenBalanceIsAtLeastAmount() {
        assertTrue(TransactionAmounts.covers(new BigDecimal("100.00"), new BigDecimal("100")));
        assertFalse(TransactionAmounts.covers(new BigDecimal("99.99"), new BigDecimal("100")));
        assertFalse(TransactionAmounts.covers(null, new BigDecimal("1")));
    }
}
