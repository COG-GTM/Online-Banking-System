package com.userfront.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class TransferAmountTest {

    @Test
    public void acceptsPositiveAmountsWithUpToTwoDecimals() {
        assertEquals(new BigDecimal("25"), TransferAmount.parse("25"));
        assertEquals(new BigDecimal("0.01"), TransferAmount.parse("0.01"));
        assertEquals(new BigDecimal("1234.50"), TransferAmount.parse(" 1234.50 "));
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsNegativeAmount() {
        TransferAmount.parse("-100");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsZero() {
        TransferAmount.parse("0.00");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsNonNumeric() {
        TransferAmount.parse("abc");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsExponentNotation() {
        TransferAmount.parse("1E100000000");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsMoreThanTwoDecimals() {
        TransferAmount.parse("1.005");
    }

    @Test(expected = InvalidTransferException.class)
    public void rejectsBlankOrMissing() {
        TransferAmount.parse(null);
    }
}
