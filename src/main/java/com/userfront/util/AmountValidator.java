package com.userfront.util;

import java.math.BigDecimal;

public final class AmountValidator {

    public static final int MAX_SCALE = 2;

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private AmountValidator() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required.");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a valid number.");
        }

        return validate(value);
    }

    public static BigDecimal validate(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Amount is required.");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (value.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than " + MAX_SCALE + " decimal places.");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount cannot exceed " + MAX_AMOUNT.toPlainString() + ".");
        }
        return value.setScale(MAX_SCALE);
    }

    public static BigDecimal validate(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Amount must be a valid number.");
        }
        return validate(BigDecimal.valueOf(value));
    }
}
