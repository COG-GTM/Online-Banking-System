package com.userfront.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class TransactionAmountTest {

    @Test
    public void parsesPositiveAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("100.00"), TransactionAmount.parse("100"));
        assertEquals(new BigDecimal("0.50"), TransactionAmount.parse("0.5"));
        assertEquals(new BigDecimal("12.34"), TransactionAmount.parse(" 12.34 "));
        assertEquals(new BigDecimal("1000000000.00"), TransactionAmount.parse("1000000000"));
    }

    @Test
    public void rejectsMalformedNonPositiveOrOverPreciseInput() {
        String[] invalid = {null, "", "  ", "-100", "-0.01", "0", "0.00", "+5", "1.234", "1.230",
                "1e3", "1E+3", "NaN", "Infinity", "abc", "1,000", ".5", "5.", "0x10",
                "1000000000.01", "99999999999"};
        for (String raw : invalid) {
            try {
                TransactionAmount.parse(raw);
                fail("expected rejection of " + raw);
            } catch (InvalidAmountException expected) {
            }
        }
    }

    @Test
    public void requireValidRejectsNegativeZeroNullAndFractionalCents() {
        BigDecimal[] invalid = {null, BigDecimal.ZERO, new BigDecimal("-1"), new BigDecimal("0.001"),
                new BigDecimal("1000000000.01")};
        for (BigDecimal amount : invalid) {
            try {
                TransactionAmount.requireValid(amount);
                fail("expected rejection of " + amount);
            } catch (InvalidAmountException expected) {
            }
        }
        assertEquals(new BigDecimal("2.50"), TransactionAmount.requireValid(new BigDecimal("2.500")));
    }
}
