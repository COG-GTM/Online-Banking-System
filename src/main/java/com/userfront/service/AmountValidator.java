package com.userfront.service;

import java.math.BigDecimal;

public final class AmountValidator {

    private static final int MAX_DECIMAL_PLACES = 2;

    private AmountValidator() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Amount must be a valid number.");
        }

        if (value.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (value.stripTrailingZeros().scale() > MAX_DECIMAL_PLACES) {
            throw new InvalidAmountException("Amount cannot have more than " + MAX_DECIMAL_PLACES + " decimal places.");
        }

        return value;
    }

    public static class InvalidAmountException extends IllegalArgumentException {

        private static final long serialVersionUID = 1L;

        public InvalidAmountException(String message) {
            super(message);
        }
    }
}
