package com.userfront.util;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class AmountValidatorTest {

    @Test
    public void acceptsPositiveAmounts() {
        assertEquals(new BigDecimal("100.00"), AmountValidator.parse("100"));
        assertEquals(new BigDecimal("12.50"), AmountValidator.parse(" 12.5 "));
        assertEquals(new BigDecimal("0.01"), AmountValidator.parse("0.01"));
        assertEquals(new BigDecimal("1.00"), AmountValidator.parse("1.000"));
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNegativeAmount() {
        AmountValidator.parse("-100");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsZero() {
        AmountValidator.parse("0");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsMoreThanTwoDecimalPlaces() {
        AmountValidator.parse("1.005");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNonNumeric() {
        AmountValidator.parse("abc");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsEmpty() {
        AmountValidator.parse(" ");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNull() {
        AmountValidator.parse(null);
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsHugeExponent() {
        AmountValidator.parse("1E+999999999");
    }

    @Test(expected = InvalidAmountException.class)
    public void rejectsNaN() {
        AmountValidator.parse(String.valueOf(Double.NaN));
    }
}
