package com.userfront.service;

import java.math.BigDecimal;

import com.userfront.exception.InvalidAmountException;

public final class Amounts {

    public static final int SCALE = 2;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private Amounts() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidAmountException("An amount is required");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Amount is not a valid number");
        }

        if (parsed.scale() > SCALE) {
            throw new InvalidAmountException("Amount must not have more than " + SCALE + " decimal places");
        }
        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount exceeds the per-transaction limit of " + MAX_AMOUNT);
        }

        return parsed.setScale(SCALE, java.math.RoundingMode.UNNECESSARY);
    }
}
