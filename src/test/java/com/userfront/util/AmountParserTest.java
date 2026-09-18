package com.userfront.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

import com.userfront.exception.InvalidAmountException;

public class AmountParserTest {

    @Test
    public void parsesPositiveAmountWithTwoDecimals() throws InvalidAmountException {
        assertEquals(new BigDecimal("10.55"), AmountParser.parse("10.55"));
        assertEquals(new BigDecimal("10.00"), AmountParser.parse("10"));
        assertEquals(new BigDecimal("10.50"), AmountParser.parse(" 10.5 "));
    }

    @Test
    public void rejectsInvalidAmounts() {
        assertRejected(null);
        assertRejected("");
        assertRejected("abc");
        assertRejected("-50");
        assertRejected("0");
        assertRejected("1.005");
    }

    private void assertRejected(String amount) {
        try {
            AmountParser.parse(amount);
            fail("Expected InvalidAmountException for " + amount);
        } catch (InvalidAmountException expected) {
            // expected
        }
    }
}
