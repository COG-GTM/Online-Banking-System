package com.userfront.util;

import java.math.BigDecimal;

public final class MonetaryAmount {

    public static final int SCALE = 2;

    private MonetaryAmount() {
    }

    /**
     * Parses a user supplied amount, rejecting anything that is not a strictly positive
     * value with at most two decimal places.
     */
    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number", e);
        }

        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (value.scale() > SCALE) {
            throw new IllegalArgumentException("Amount must have at most " + SCALE + " decimal places");
        }

        return value.setScale(SCALE);
    }
}
