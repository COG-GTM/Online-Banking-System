package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class AmountValidator {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    public static final int MAX_SCALE = 2;

    private static final Pattern AMOUNT_FORMAT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    private AmountValidator() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidTransactionException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!AMOUNT_FORMAT.matcher(value).matches()) {
            throw new InvalidTransactionException("Amount must be a positive number with at most two decimal places.");
        }
        return requireValid(new BigDecimal(value));
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("Please enter an amount.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidTransactionException("Amount must have at most two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidTransactionException("Amount exceeds the per-transaction limit of $" + MAX_AMOUNT.toPlainString() + ".");
        }
        return amount.setScale(MAX_SCALE);
    }
}
