package com.userfront.validation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class TransactionAmount {

    public static final int SCALE = 2;
    public static final BigDecimal MAX = new BigDecimal("1000000000.00");

    private static final Pattern FORMAT = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    private TransactionAmount() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidAmountException("Amount must be a positive number with at most two decimal places.");
        }
        return requireValid(new BigDecimal(value));
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidAmountException("Amount must have at most two decimal places.");
        }
        if (amount.compareTo(MAX) > 0) {
            throw new InvalidAmountException("Amount must not exceed " + MAX.toPlainString() + ".");
        }
        return amount.setScale(SCALE);
    }
}
