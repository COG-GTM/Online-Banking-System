package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class MoneyAmount {

    public static final int SCALE = 2;
    public static final BigDecimal MAX = new BigDecimal("1000000.00");

    private static final Pattern FORMAT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    private MoneyAmount() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidTransactionException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidTransactionException("Amount must be a positive number with at most two decimal places.");
        }
        return requireValid(new BigDecimal(value));
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidTransactionException("Amount must have at most two decimal places.");
        }
        if (amount.compareTo(MAX) > 0) {
            throw new InvalidTransactionException("Amount must not exceed " + MAX.toPlainString() + ".");
        }
        return amount.setScale(SCALE);
    }
}
