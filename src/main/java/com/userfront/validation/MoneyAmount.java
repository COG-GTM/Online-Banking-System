package com.userfront.validation;

import java.math.BigDecimal;

public final class MoneyAmount {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");

    private static final int MAX_SCALE = 2;
    private static final int MAX_INPUT_LENGTH = 16;

    private MoneyAmount() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }
        String value = raw.trim();
        if (value.length() > MAX_INPUT_LENGTH || !value.matches("\\d+(\\.\\d+)?")) {
            throw new InvalidAmountException("Amount must be a positive number with at most two decimal places.");
        }
        return requireValid(new BigDecimal(value));
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidAmountException("Amount must have at most two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount must not exceed " + MAX_AMOUNT.toPlainString() + ".");
        }
        return amount.setScale(MAX_SCALE);
    }
}
