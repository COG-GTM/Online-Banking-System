package com.userfront.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.userfront.exception.InvalidAmountException;

class AmountsTest {

    @Test
    void parsesPositiveAmountWithTwoDecimals() {
        assertEquals(new BigDecimal("12.34"), Amounts.parse(" 12.34 "));
    }

    @Test
    void normalisesScale() {
        assertEquals(new BigDecimal("12.00"), Amounts.parse("12"));
    }

    @Test
    void rejectsBlankAmount() {
        assertThrows(InvalidAmountException.class, () -> Amounts.parse("  "));
    }

    @Test
    void rejectsNonNumericAmount() {
        assertThrows(InvalidAmountException.class, () -> Amounts.parse("1.0e999x"));
    }

    @Test
    void rejectsNegativeAndZeroAmounts() {
        assertThrows(InvalidAmountException.class, () -> Amounts.parse("-1.00"));
        assertThrows(InvalidAmountException.class, () -> Amounts.parse("0"));
    }

    @Test
    void rejectsTooManyDecimalPlaces() {
        assertThrows(InvalidAmountException.class, () -> Amounts.parse("1.005"));
    }

    @Test
    void rejectsAmountAboveLimit() {
        assertThrows(InvalidAmountException.class, () -> Amounts.parse("1000000.01"));
    }
}
