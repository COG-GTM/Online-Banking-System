package com.userfront.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.Test;

public class AmountParserTest {

    @Test
    public void parsesDecimalExactly() {
        Optional<BigDecimal> parsed = AmountParser.parse("0.1");

        assertTrue(parsed.isPresent());
        assertEquals(new BigDecimal("0.1"), parsed.get());
    }

    @Test
    public void trimsSurroundingWhitespace() {
        assertEquals(new BigDecimal("12.34"), AmountParser.parse("  12.34  ").get());
    }

    @Test
    public void rejectsNonNumericInput() {
        assertFalse(AmountParser.parse("abc").isPresent());
    }

    @Test
    public void rejectsNull() {
        assertFalse(AmountParser.parse(null).isPresent());
    }

    @Test
    public void rejectsEmptyInput() {
        assertFalse(AmountParser.parse("   ").isPresent());
    }

    @Test
    public void rejectsZeroAndNegativeAmounts() {
        assertFalse(AmountParser.parse("0").isPresent());
        assertFalse(AmountParser.parse("-5.00").isPresent());
    }
}
