package com.userfront.service.transfer;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class TransferAmountTest {

    @Test
    public void parsesPositiveAmounts() {
        assertEquals(new BigDecimal("100"), TransferAmount.parse("100"));
        assertEquals(new BigDecimal("0.01"), TransferAmount.parse(" 0.01 "));
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsNegativeAmount() {
        TransferAmount.parse("-1000000");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsZero() {
        TransferAmount.parse("0.00");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsScientificNotation() {
        TransferAmount.parse("1E100000000");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsMoreThanTwoDecimals() {
        TransferAmount.parse("1.001");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsBlankAndNull() {
        TransferAmount.parse(null);
    }

    @Test(expected = InsufficientFundsException.class)
    public void requireCoveredRejectsOverdraft() {
        TransferAmount.requireCovered(new BigDecimal("10"), new BigDecimal("10.01"));
    }

    @Test
    public void requireCoveredAllowsExactBalance() {
        TransferAmount.requireCovered(new BigDecimal("10.00"), new BigDecimal("10"));
    }
}
