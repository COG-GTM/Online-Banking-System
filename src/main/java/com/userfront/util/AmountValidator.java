package com.userfront.util;

import java.math.BigDecimal;

public final class AmountValidator {

    private static final int MAX_SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

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
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount exceeds the maximum allowed.");
        }
        if (value.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidAmountException("Amount must not have more than two decimal places.");
        }

        return value.setScale(MAX_SCALE);
    }
}
