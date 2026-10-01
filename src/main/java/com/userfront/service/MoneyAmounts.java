package com.userfront.service;

import java.math.BigDecimal;

public final class MoneyAmounts {

    private static final int SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private MoneyAmounts() {
    }

    public static BigDecimal parse(String rawAmount) {
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            throw new TransactionRejectedException("Please enter an amount.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(rawAmount.trim());
        } catch (NumberFormatException e) {
            throw new TransactionRejectedException("Amount must be a number, for example 25.00.");
        }
        return requireValid(amount);
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransactionRejectedException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new TransactionRejectedException("Amount cannot exceed 1,000,000,000.00 per transaction.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new TransactionRejectedException("Amount cannot have more than two decimal places.");
        }
        return amount.setScale(SCALE);
    }

    public static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new TransactionRejectedException("Insufficient funds: the amount exceeds the available balance.");
        }
    }
}
