package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;

import com.userfront.service.TransactionDeclinedException;

public class AccountLedgerTest {

    @Test
    public void acceptsPositiveAmountsWithAtMostTwoDecimals() {
        assertEquals(new BigDecimal("10.00"), AccountLedger.validAmount("10"));
        assertEquals(new BigDecimal("0.10"), AccountLedger.validAmount(0.1));
        assertEquals(new BigDecimal("12.50"), AccountLedger.validAmount(" 12.5 "));
    }

    @Test
    public void rejectsNonPositiveMalformedAndFractionalCentAmounts() {
        for (String raw : new String[] {"0", "-5", "abc", "", "1.005", "NaN", null}) {
            try {
                AccountLedger.validAmount(raw);
                fail("expected decline for " + raw);
            } catch (TransactionDeclinedException expected) {
            }
        }
        for (double raw : new double[] {0, -0.01, Double.NaN, Double.POSITIVE_INFINITY}) {
            try {
                AccountLedger.validAmount(raw);
                fail("expected decline for " + raw);
            } catch (TransactionDeclinedException expected) {
            }
        }
    }
}
