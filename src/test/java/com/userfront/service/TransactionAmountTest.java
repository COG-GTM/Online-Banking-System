package com.userfront.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class TransactionAmountTest {

    @Test
    public void acceptsPositiveAmountsWithUpToTwoDecimals() {
        assertEquals(new BigDecimal("25.00"), TransactionAmount.parse("25"));
        assertEquals(new BigDecimal("25.50"), TransactionAmount.parse(" 25.5 "));
        assertEquals(new BigDecimal("0.01"), TransactionAmount.parse("0.01"));
        assertEquals(new BigDecimal("1000000.00"), TransactionAmount.parse("1000000"));
    }

    @Test
    public void acceptsCommaAsDecimalSeparator() {
        assertEquals(new BigDecimal("25.50"), TransactionAmount.parse("25,50"));
        assertEquals(new BigDecimal("0.50"), TransactionAmount.parse("0,5"));
    }

    @Test
    public void rejectsNegativeZeroMalformedAndOutOfRangeAmounts() {
        String[] rejected = {null, "", "  ", "-1000000", "-0.01", "0", "0.00", "+5", "1.001", "1e6", "1E-2",
                "NaN", "Infinity", "1,000", "1,000.00", "1,5,0", "abc", "1000000.01", "99999999", "0x10", ".5", "5."};
        for (String raw : rejected) {
            try {
                TransactionAmount.parse(raw);
                fail("expected rejection for " + raw);
            } catch (TransactionDeclinedException expected) {
            }
        }
    }

    @Test
    public void requireValidRejectsNegativeBigDecimal() {
        assertThrows(TransactionDeclinedException.class, () -> TransactionAmount.requireValid(new BigDecimal("-5")));
    }

    @Test
    public void requireValidRejectsThreeDecimals() {
        assertThrows(TransactionDeclinedException.class, () -> TransactionAmount.requireValid(new BigDecimal("5.125")));
    }

    @Test
    public void requireValidRejectsNull() {
        assertThrows(TransactionDeclinedException.class, () -> TransactionAmount.requireValid(null));
    }
}
