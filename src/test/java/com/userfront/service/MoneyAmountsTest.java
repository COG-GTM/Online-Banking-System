package com.userfront.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class MoneyAmountsTest {

    @Test
    public void parsesPositiveAmountsToTwoDecimals() {
        assertEquals(new BigDecimal("25.00"), MoneyAmounts.parse("25"));
        assertEquals(new BigDecimal("0.10"), MoneyAmounts.parse(" 0.1 "));
        assertEquals(new BigDecimal("12.34"), MoneyAmounts.parse("12.340"));
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsNegativeAmount() {
        MoneyAmounts.parse("-100");
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsZeroAmount() {
        MoneyAmounts.parse("0.00");
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsMoreThanTwoDecimals() {
        MoneyAmounts.parse("1.005");
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsNonNumericAmount() {
        MoneyAmounts.parse("abc");
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsBlankAmount() {
        MoneyAmounts.parse("  ");
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsHugeAmount() {
        MoneyAmounts.parse("1E+30");
    }

    @Test(expected = TransactionRejectedException.class)
    public void rejectsDebitAboveBalance() {
        MoneyAmounts.requireSufficientFunds(new BigDecimal("10.00"), new BigDecimal("10.01"));
    }

    @Test
    public void allowsDebitOfEntireBalance() {
        MoneyAmounts.requireSufficientFunds(new BigDecimal("10.00"), new BigDecimal("10.00"));
    }
}
