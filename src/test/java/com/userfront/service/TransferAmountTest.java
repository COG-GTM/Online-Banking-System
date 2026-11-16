package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class TransferAmountTest {

    @Test
    public void acceptsPlainDecimals() {
        assertEquals(new BigDecimal("125.50"), TransferAmount.parse("125.50"));
        assertEquals(new BigDecimal("7"), TransferAmount.parse(" 7 "));
        assertEquals(new BigDecimal("0.01"), TransferAmount.parse("0.01"));
        assertEquals(new BigDecimal("999999999999.99"), TransferAmount.parse("999999999999.99"));
    }

    @Test(timeout = 1000)
    public void rejectsHugeExponentsWithoutDoingArithmetic() {
        assertRejected("1e1000000000");
        assertRejected("1E100000000");
        assertRejected("1e-1000000000");
        assertRejected("9.99e+999999999");
    }

    @Test
    public void rejectsMalformedOrOutOfRangeAmounts() {
        assertRejected(null);
        assertRejected("");
        assertRejected("   ");
        assertRejected("0");
        assertRejected("0.00");
        assertRejected("-5");
        assertRejected("+5");
        assertRejected("1.234");
        assertRejected("1,000");
        assertRejected(".5");
        assertRejected("5.");
        assertRejected("NaN");
        assertRejected("Infinity");
        assertRejected("0x10");
        assertRejected("1000000000000");
        assertRejected("12345678901234567890");
    }

    private static void assertRejected(String raw) {
        try {
            TransferAmount.parse(raw);
            fail("expected rejection of " + raw);
        } catch (InvalidTransferAmountException expected) {
            assertEquals(TransferAmount.MESSAGE, expected.getMessage());
        }
    }
}
