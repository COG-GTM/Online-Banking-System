package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Parsing and validation of user supplied monetary amounts.
 */
public final class MoneyUtil {

    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

    private MoneyUtil() {
    }

    /**
     * Parses an amount entered by a user and rejects anything that is not a
     * strictly positive value within the allowed range.
     */
    public static BigDecimal parsePositiveAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number");
        }

        if (value.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed per operation");
        }

        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
