package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class AmountParser {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

    private static final Pattern AMOUNT_FORMAT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    private AmountParser() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!AMOUNT_FORMAT.matcher(value).matches()) {
            throw new InvalidAmountException("Amount must be a positive number with at most two decimal places, e.g. 125.50.");
        }
        BigDecimal amount = new BigDecimal(value).setScale(2);
        requireValid(amount);
        return amount;
    }

    public static void requireValid(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount must have at most two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount must not exceed $1,000,000.00.");
        }
    }
}
