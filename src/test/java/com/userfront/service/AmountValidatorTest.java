package com.userfront.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

import com.userfront.service.AmountValidator.InvalidAmountException;

public class AmountValidatorTest {

    @Test
    public void acceptsPositiveAmounts() {
        assertEquals(new BigDecimal("100"), AmountValidator.parse("100"));
        assertEquals(new BigDecimal("0.01"), AmountValidator.parse("0.01"));
        assertEquals(new BigDecimal("12.50"), AmountValidator.parse(" 12.50 "));
        assertEquals(new BigDecimal("5.000"), AmountValidator.parse("5.000"));
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNegativeAmount() {
        AmountValidator.parse("-100");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsZero() {
        AmountValidator.parse("0.00");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsMoreThanTwoDecimalPlaces() {
        AmountValidator.parse("1.001");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNonNumeric() {
        AmountValidator.parse("NaN");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsBlank() {
        AmountValidator.parse("  ");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNull() {
        AmountValidator.parse(null);
    }
}
