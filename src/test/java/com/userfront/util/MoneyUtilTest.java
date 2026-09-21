package com.userfront.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyUtilTest {

    @Test
    void parsesPositiveAmount() {
        assertEquals(new BigDecimal("10.50"), MoneyUtil.parsePositiveAmount(" 10.50 "));
    }

    @Test
    void normalisesScale() {
        assertEquals(new BigDecimal("10.00"), MoneyUtil.parsePositiveAmount("10"));
    }

    @Test
    void rejectsNegativeAndZeroAmounts() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parsePositiveAmount("-1"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parsePositiveAmount("0"));
    }

    @Test
    void rejectsNonNumericAndSubCentAmounts() {
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parsePositiveAmount("abc"));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parsePositiveAmount(""));
        assertThrows(IllegalArgumentException.class, () -> MoneyUtil.parsePositiveAmount("1.005"));
    }
}
