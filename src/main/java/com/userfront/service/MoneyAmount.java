package com.userfront.service;

import java.math.BigDecimal;

/**
 * Parsing and validation of user supplied monetary amounts.
 */
public final class MoneyAmount {

    public static final int SCALE = 2;

    private MoneyAmount() {
    }

    /**
     * Parses a monetary amount, rejecting non-numeric input, non-positive values
     * and values with more than two decimal places.
     */
    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidAmountException("An amount is required");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Amount must be a decimal number");
        }

        if (value.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }

        if (value.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidAmountException("Amount must not have more than " + SCALE + " decimal places");
        }

        return value.setScale(SCALE);
    }
}
