package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class TransferAmount {

    public static final BigDecimal MAX = new BigDecimal("1000000.00");

    private static final Pattern FORMAT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    private TransferAmount() {
    }

    /**
     * Parses a user-supplied amount: plain decimal, at most 2 fraction digits,
     * strictly positive and no larger than {@link #MAX}.
     */
    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidTransferException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidTransferException("Amount must be a positive number with at most 2 decimal places.");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) {
            throw new InvalidTransferException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX) > 0) {
            throw new InvalidTransferException("Amount exceeds the per-transfer limit of " + MAX.toPlainString() + ".");
        }
        return amount;
    }

    public static void requireCovered(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException();
        }
    }
}
