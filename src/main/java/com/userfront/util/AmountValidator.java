package com.userfront.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class AmountValidator {

    public static final int MAX_SCALE = 2;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");
    private static final int MAX_INPUT_LENGTH = 32;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d{1,10}(\\.\\d+)?");

    private AmountValidator() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        String trimmed = amount.trim();
        if (trimmed.startsWith("-")) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (trimmed.length() > MAX_INPUT_LENGTH || !AMOUNT_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Amount must be a valid number");
        }
        return validate(new BigDecimal(trimmed));
    }

    public static BigDecimal parse(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Amount must be a valid number");
        }
        return validate(BigDecimal.valueOf(amount));
    }

    private static BigDecimal validate(BigDecimal value) {
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (value.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than " + MAX_SCALE + " decimal places");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed");
        }
        return value.setScale(MAX_SCALE);
    }
}
