package com.userfront.util;

import java.math.BigDecimal;

public final class MoneyUtil {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private MoneyUtil() {
    }

    /**
     * Parses a user supplied monetary amount, accepting only strictly positive values
     * with at most two decimal places.
     */
    public static BigDecimal parsePositiveAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number");
        }

        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (value.scale() > 2) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }

        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed value");
        }

        return value.setScale(2, BigDecimal.ROUND_UNNECESSARY);
    }

    public static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }
}
