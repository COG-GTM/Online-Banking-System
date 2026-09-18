package com.userfront.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyAmountTest {

    @Test
    void parsesDecimalAmountWithTwoDecimalPlaces() {
        assertEquals(new BigDecimal("10.05"), MoneyAmount.parse("10.05"));
    }

    @Test
    void normalisesScaleToTwoDecimalPlaces() {
        assertEquals(new BigDecimal("10.00"), MoneyAmount.parse("10"));
    }

    @Test
    void rejectsNonNumericInput() {
        assertThrows(InvalidAmountException.class, () -> MoneyAmount.parse("abc"));
    }

    @Test
    void rejectsBlankInput() {
        assertThrows(InvalidAmountException.class, () -> MoneyAmount.parse("  "));
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThrows(InvalidAmountException.class, () -> MoneyAmount.parse("0"));
        assertThrows(InvalidAmountException.class, () -> MoneyAmount.parse("-1.00"));
    }

    @Test
    void rejectsMoreThanTwoDecimalPlaces() {
        assertThrows(InvalidAmountException.class, () -> MoneyAmount.parse("1.005"));
    }
}
