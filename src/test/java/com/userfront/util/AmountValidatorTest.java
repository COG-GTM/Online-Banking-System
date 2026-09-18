package com.userfront.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

import com.userfront.exception.InvalidTransactionException;

public class AmountValidatorTest {

    @Test
    public void acceptsPositiveAmountsAndNormalizesToTwoDecimals() throws Exception {
        assertEquals(new BigDecimal("100.00"), AmountValidator.parse("100"));
        assertEquals(new BigDecimal("1.50"), AmountValidator.parse("1.5"));
        assertEquals(new BigDecimal("0.01"), AmountValidator.parse(" 0.01 "));
    }

    @Test
    public void rejectsInvalidAmounts() {
        String[] invalid = {null, "", "   ", "abc", "0", "-50", "-0.01", "1.234", "1e2147483647", "1000000000000000.00"};

        for (String amount : invalid) {
            try {
                AmountValidator.parse(amount);
                fail("Expected rejection of amount: " + amount);
            } catch (InvalidTransactionException expected) {
                // expected
            }
        }
    }
}
