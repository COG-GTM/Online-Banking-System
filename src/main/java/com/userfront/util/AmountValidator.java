package com.userfront.util;

import java.math.BigDecimal;

/**
 * Server side validation for monetary amounts submitted through the web forms.
 */
public final class AmountValidator {

    private static final int MAX_SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private AmountValidator() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount.");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid amount.");
        }

        return validate(parsed);
    }

    public static BigDecimal validate(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed for a single transaction.");
        }

        return amount;
    }
}
