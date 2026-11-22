package com.userfront.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class MonetaryAmountsTest {

    @Test
    public void parsesValidAmountsToFixedScale() {
        assertEquals(new BigDecimal("125.50"), MonetaryAmounts.parse("125.5"));
        assertEquals(new BigDecimal("0.10"), MonetaryAmounts.parse(" 0.1 "));
        assertEquals(new BigDecimal("100.00"), MonetaryAmounts.parse("100"));
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsNegativeAmount() {
        MonetaryAmounts.parse("-500");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsZero() {
        MonetaryAmounts.parse("0.00");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsNonNumeric() {
        MonetaryAmounts.parse("abc");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsNaN() {
        MonetaryAmounts.parse("NaN");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsExponentNotation() {
        MonetaryAmounts.parse("1e3");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsMoreThanTwoDecimals() {
        MonetaryAmounts.parse("1.005");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsBlank() {
        MonetaryAmounts.parse("  ");
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsAmountAboveMaximum() {
        MonetaryAmounts.parse("1000000000.01");
    }

    @Test(expected = InvalidTransactionException.class)
    public void requireValidRejectsNegativeBigDecimal() {
        MonetaryAmounts.requireValid(new BigDecimal("-1.00"));
    }

    @Test(expected = InvalidTransactionException.class)
    public void rejectsInsufficientFunds() {
        MonetaryAmounts.requireSufficientFunds(new BigDecimal("10.00"), new BigDecimal("10.01"));
    }

    @Test
    public void allowsExactBalance() {
        MonetaryAmounts.requireSufficientFunds(new BigDecimal("10.00"), new BigDecimal("10.00"));
    }
}
