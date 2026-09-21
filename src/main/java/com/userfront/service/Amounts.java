package com.userfront.service;

import java.math.BigDecimal;

public final class Amounts {

    private Amounts() {
    }

    public static BigDecimal parsePositive(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number: " + amount);
        }

        return validatePositive(value);
    }

    public static BigDecimal validatePositive(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount must not have more than two decimal places");
        }

        return amount;
    }
}
