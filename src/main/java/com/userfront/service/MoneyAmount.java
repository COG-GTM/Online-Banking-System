package com.userfront.service;

import java.math.BigDecimal;

public final class MoneyAmount {

    public static final int SCALE = 2;

    private MoneyAmount() {
    }

    /**
     * Parses a user supplied monetary amount and rejects anything that is not a
     * strictly positive value with at most {@value #SCALE} decimal places.
     */
    public static BigDecimal parsePositive(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new InvalidTransactionException("Amount is required");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("Amount is not a valid number");
        }

        if (value.signum() <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero");
        }

        if (value.scale() > SCALE) {
            throw new InvalidTransactionException("Amount must have at most " + SCALE + " decimal places");
        }

        return value.setScale(SCALE);
    }

    public static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new InvalidTransactionException("Insufficient funds");
        }
    }
}
