package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class TransferAmountTest {

    @Test
    public void parsesPlainPositiveAmounts() {
        assertEquals(new BigDecimal("10"), TransferAmount.parse("10"));
        assertEquals(new BigDecimal("0.01"), TransferAmount.parse(" 0.01 "));
        assertEquals(new BigDecimal("1000000.00"), TransferAmount.parse("1000000.00"));
    }

    @Test
    public void rejectsInvalidAmounts() {
        String[] invalid = {null, "", "  ", "-1000000", "-0.01", "0", "0.00", "+5", "1e3", "1E-2",
                "10.001", "abc", "NaN", "Infinity", "1,000", "1000000.01", "99999999"};
        for (String raw : invalid) {
            try {
                TransferAmount.parse(raw);
                fail("expected rejection of " + raw);
            } catch (InvalidTransferException expected) {
                // ok
            }
        }
    }

    @Test(expected = InsufficientFundsException.class)
    public void requireCoveredRejectsAmountAboveBalance() {
        TransferAmount.requireCovered(new BigDecimal("50.00"), new BigDecimal("50.01"));
    }

    @Test
    public void requireCoveredAllowsExactBalance() {
        TransferAmount.requireCovered(new BigDecimal("50.00"), new BigDecimal("50"));
    }
}
