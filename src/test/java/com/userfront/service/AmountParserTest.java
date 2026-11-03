package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class AmountParserTest {

    @Test
    public void parsesValidAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("125.50"), AmountParser.parse("125.5"));
        assertEquals(new BigDecimal("0.01"), AmountParser.parse("0.01"));
        assertEquals(new BigDecimal("40.00"), AmountParser.parse(" 40 "));
        assertEquals(new BigDecimal("1000000.00"), AmountParser.parse("1000000"));
    }

    @Test
    public void rejectsMalformedSignedAndOutOfRangeAmounts() {
        String[] invalid = {null, "", "   ", "-50", "+50", "0", "0.00", "abc", "NaN", "Infinity", "-Infinity",
                "1e3", "1E3", "0x10", "1,000", "10.", ".5", "10.001", "1000000.01", "99999999", "50 00", "١٠"};
        for (String raw : invalid) {
            try {
                AmountParser.parse(raw);
                fail("expected rejection of [" + raw + "]");
            } catch (InvalidAmountException expected) {
                // rejected as intended
            }
        }
    }

    @Test(expected = InvalidAmountException.class)
    public void requireValidRejectsNegativeAmounts() {
        AmountParser.requireValid(new BigDecimal("-0.01"));
    }

    @Test(expected = InvalidAmountException.class)
    public void requireValidRejectsSubCentPrecision() {
        AmountParser.requireValid(new BigDecimal("1.005"));
    }

    @Test(expected = InvalidAmountException.class)
    public void requireValidRejectsNull() {
        AmountParser.requireValid(null);
    }
}
