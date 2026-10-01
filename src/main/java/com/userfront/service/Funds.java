package com.userfront.service;

import java.math.BigDecimal;

public final class Funds {

    private Funds() {}

    public static BigDecimal parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        try {
            return requirePositive(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount must be a number.");
        }
    }

    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return amount;
    }

    public static boolean covers(BigDecimal balance, BigDecimal amount) {
        return balance != null && balance.compareTo(amount) >= 0;
    }
}
