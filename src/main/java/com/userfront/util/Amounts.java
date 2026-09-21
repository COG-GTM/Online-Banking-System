package com.userfront.util;

import java.math.BigDecimal;

public final class Amounts {

    private static final int SCALE = 2;

    private Amounts() {}

    public static BigDecimal parsePositive(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number");
        }

        return requirePositive(parsed);
    }

    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.scale() > SCALE) {
            throw new IllegalArgumentException("Amount supports at most " + SCALE + " decimal places");
        }
        return amount.setScale(SCALE, BigDecimal.ROUND_UNNECESSARY);
    }

    public static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }
}
