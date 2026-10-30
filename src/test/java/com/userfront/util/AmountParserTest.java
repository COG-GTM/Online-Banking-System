package com.userfront.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class AmountParserTest {

    @Test
    public void parsesDecimalAmountsExactly() {
        assertEquals(new BigDecimal("0.10"), AmountParser.parse("0.1"));
        assertEquals(new BigDecimal("100.00"), AmountParser.parse(" 100 "));
        assertEquals(new BigDecimal("19.99"), AmountParser.parse("19.99"));
        assertEquals(AmountParser.MAX_AMOUNT, AmountParser.parse("1000000000"));
    }

    @Test
    public void repeatedDepositsDoNotDrift() {
        BigDecimal balance = BigDecimal.ZERO.setScale(AmountParser.SCALE);
        for (int i = 0; i < 10; i++) {
            balance = balance.add(AmountParser.parse("0.1"));
        }
        assertEquals(new BigDecimal("1.00"), balance);
    }

    @Test
    public void rejectsInvalidInput() {
        String[] invalid = {null, "", "   ", "abc", "1,000", "-5", "+5", "0", "0.00", "1.001", ".5", "5.",
                "1e3", "1E-600000000", "NaN", "Infinity", "0x10", "1000000000.01", "99999999999"};
        for (String amount : invalid) {
            try {
                AmountParser.parse(amount);
                fail("Expected InvalidAmountException for " + amount);
            } catch (InvalidAmountException expected) {
                // expected
            }
        }
    }
}
