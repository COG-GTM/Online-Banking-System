package com.userfront.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MonetaryAmountTest {

    @Test
    void parsesPositiveAmountWithTwoDecimals() {
        assertEquals(new BigDecimal("10.50"), MonetaryAmount.parse(" 10.5 "));
    }

    @Test
    void rejectsBlankAmount() {
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("  "));
    }

    @Test
    void rejectsNonNumericAmount() {
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("1,00"));
    }

    @Test
    void rejectsZeroAndNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("0"));
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("-5.00"));
    }

    @Test
    void rejectsSubCentPrecision() {
        assertThrows(IllegalArgumentException.class, () -> MonetaryAmount.parse("1.005"));
    }
}
