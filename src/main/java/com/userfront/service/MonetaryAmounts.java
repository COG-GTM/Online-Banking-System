package com.userfront.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class MonetaryAmounts {

    public static final int SCALE = 2;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d{1,10}(\\.\\d{1,2})?$");

    private MonetaryAmounts() {}

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidTransactionException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!AMOUNT_PATTERN.matcher(value).matches()) {
            throw new InvalidTransactionException("Please enter a positive amount with at most two decimal places, e.g. 125.50.");
        }
        return requireValid(new BigDecimal(value));
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("Please enter an amount.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidTransactionException("The amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidTransactionException("The amount can have at most two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidTransactionException("The amount exceeds the maximum allowed per transaction.");
        }
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        BigDecimal available = balance == null ? BigDecimal.ZERO : balance;
        if (available.compareTo(amount) < 0) {
            throw new InvalidTransactionException("Insufficient funds: the amount exceeds the available balance.");
        }
    }
}
