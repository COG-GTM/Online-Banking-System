package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {

    private MoneyUtil() {
    }

    public static BigDecimal parseAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Amount is not a valid number");
        }

        if (parsed.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (parsed.scale() > 2) {
            throw new IllegalArgumentException("Amount must have at most two decimal places");
        }

        return parsed.setScale(2, RoundingMode.UNNECESSARY);
    }
}
