package com.userfront.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int PRECISION = 19;
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    private static final int MAX_INTEGER_DIGITS = PRECISION - SCALE;

    private Money() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number");
        }
        return normalize(value);
    }

    public static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        requireFitsColumn(value);
        BigDecimal scaled = value.setScale(SCALE, ROUNDING);
        requireFitsColumn(scaled);
        return scaled;
    }

    private static void requireFitsColumn(BigDecimal value) {
        if (value.precision() - value.scale() > MAX_INTEGER_DIGITS) {
            throw new IllegalArgumentException("Amount exceeds " + MAX_INTEGER_DIGITS + " integer digits");
        }
    }
}
