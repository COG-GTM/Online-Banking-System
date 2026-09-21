package com.userfront.util;

import java.math.BigDecimal;

/**
 * Parsing and validation of monetary amounts submitted by users.
 */
public final class MoneyAmount {

    private MoneyAmount() {
    }

    /**
     * Parses a user supplied amount, rejecting anything that is not a positive
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
            throw new IllegalArgumentException("Amount must be a valid number", e);
        }

        return validate(value);
    }

    public static BigDecimal validate(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }
        return value.setScale(2);
    }
}
