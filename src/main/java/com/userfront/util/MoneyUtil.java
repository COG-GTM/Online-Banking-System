package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    private static final int SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private MoneyUtil() {
    }

    /**
     * Parses a user supplied monetary amount and rejects anything that is not a
     * strictly positive value with at most two decimal places.
     */
    public static BigDecimal parsePositiveAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number");
        }

        if (parsed.scale() > SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than " + SCALE + " decimal places");
        }
        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed value");
        }

        return parsed.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
