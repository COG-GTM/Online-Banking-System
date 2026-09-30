package com.userfront.service;

import java.math.BigDecimal;

public final class TransactionAmounts {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

    private TransactionAmounts() {}

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number.");
        }
        return validate(value);
    }

    public static BigDecimal validate(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount cannot exceed $" + MAX_AMOUNT.toPlainString() + ".");
        }
        return amount;
    }

    public static boolean covers(BigDecimal balance, BigDecimal amount) {
        return balance != null && balance.compareTo(amount) >= 0;
    }
}
