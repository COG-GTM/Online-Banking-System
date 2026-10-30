package com.userfront.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class AmountParser {

    public static final int SCALE = 2;

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d{1,10}(\\.\\d{1,2})?");

    private AmountParser() {
    }

    public static BigDecimal parse(String amount) {
        String trimmed = amount == null ? "" : amount.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }
        if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidAmountException("Please enter a valid amount with at most two decimal places, for example 100.00.");
        }

        BigDecimal parsed = new BigDecimal(trimmed).setScale(SCALE);
        if (parsed.signum() <= 0) {
            throw new InvalidAmountException("Please enter an amount greater than zero.");
        }
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amounts cannot exceed 1,000,000,000.00.");
        }

        return parsed;
    }
}
