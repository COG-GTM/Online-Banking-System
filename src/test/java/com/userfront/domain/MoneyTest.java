package com.userfront.domain;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void parseKeepsExactDecimalValueAtFixedScale() {
        assertEquals("0.10", Money.parse("0.1").toPlainString());
        assertEquals("100.00", Money.parse(" 100 ").toPlainString());
        assertEquals("12345678901234567.89", Money.parse("12345678901234567.89").toPlainString());
    }

    @Test
    public void parseRoundsHalfEven() {
        assertEquals("1.00", Money.parse("1.005").toPlainString());
        assertEquals("1.02", Money.parse("1.015").toPlainString());
        assertEquals("2.68", Money.parse("2.675").toPlainString());
    }

    @Test
    public void normalizeAppliesScaleToArithmeticResults() {
        BigDecimal sum = Money.parse("0.10").add(Money.parse("0.20"));
        assertEquals("0.30", Money.normalize(sum).toPlainString());
        assertEquals(Money.SCALE, Money.ZERO.scale());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsBlank() {
        Money.parse("  ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsNonNumeric() {
        Money.parse("12,50");
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsValuesWiderThanTheColumn() {
        Money.parse("123456789012345678");
    }

    @Test
    public void parseAcceptsLargestValueThatFitsTheColumn() {
        assertEquals("99999999999999999.99", Money.parse("99999999999999999.994").toPlainString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsValuesThatRoundIntoAnExtraIntegerDigit() {
        Money.parse("99999999999999999.995");
    }

    @Test(expected = IllegalArgumentException.class)
    public void normalizeRejectsArithmeticResultsWiderThanTheColumn() {
        Money.normalize(Money.parse("99999999999999999.99").add(Money.parse("0.01")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseRejectsHugeExponentWithoutExpandingIt() {
        Money.parse("1E100000000");
    }
}
