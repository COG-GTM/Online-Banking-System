package com.userfront.validation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class TransactionAmount {

    public static final int SCALE = 2;

    // Plain decimal only: no sign, exponent or more than 12 integer digits, so
    // BigDecimal arithmetic against a scale-2 balance stays bounded.
    private static final Pattern FORMAT = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");

    private TransactionAmount() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }
        String value = raw.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidAmountException("Amount must be a positive number with up to 12 digits and at most two decimal places.");
        }
        BigDecimal amount = new BigDecimal(value).setScale(SCALE);
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        return amount;
    }
}
