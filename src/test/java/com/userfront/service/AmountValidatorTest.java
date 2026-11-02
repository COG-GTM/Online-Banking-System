package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

public class AmountValidatorTest {

    @Test
    public void parsesPositiveAmountsToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("5.00"), AmountValidator.parse("5"));
        assertEquals(new BigDecimal("0.01"), AmountValidator.parse(" 0.01 "));
        assertEquals(new BigDecimal("12.50"), AmountValidator.parse("12.5"));
        assertEquals(new BigDecimal("1000000.00"), AmountValidator.parse("1000000"));
    }

    @Test
    public void rejectsMalformedNonPositiveAndOversizedAmounts() {
        String[] rejected = {null, "", "   ", "-5", "-1000000", "+5", "0", "0.00", "1.001", "NaN", "Infinity",
                "-Infinity", "1e308", "1E3", "0x10", "1,000", "abc", "1000000.01", "99999999999", "5 6"};
        for (String raw : rejected) {
            try {
                AmountValidator.parse(raw);
                fail("expected rejection of " + raw);
            } catch (InvalidTransactionException expected) {
                // expected
            }
        }
    }

    @Test
    public void requireValidChecksBigDecimalsPassedDirectlyToTheService() {
        assertEquals(new BigDecimal("1000.00"), AmountValidator.requireValid(new BigDecimal("1E+3")));
        BigDecimal[] rejected = {null, new BigDecimal("-1"), BigDecimal.ZERO, new BigDecimal("0.001"),
                new BigDecimal("1.000"), new BigDecimal("1E+308")};
        for (BigDecimal amount : rejected) {
            try {
                AmountValidator.requireValid(amount);
                fail("expected rejection of " + amount);
            } catch (InvalidTransactionException expected) {
                // expected
            }
        }
    }
}
