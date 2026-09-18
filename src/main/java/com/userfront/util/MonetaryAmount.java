package com.userfront.util;

import java.math.BigDecimal;

public final class MonetaryAmount {

    public static final String INVALID_AMOUNT_MESSAGE = "Please enter a positive amount with at most two decimal places.";

    private static final int MAX_SCALE = 2;

    private MonetaryAmount() {
    }

    public static BigDecimal parse(String amount) {
        if (amount == null) {
            return null;
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            return null;
        }

        if (parsed.signum() <= 0 || parsed.scale() > MAX_SCALE) {
            return null;
        }

        return parsed;
    }
}
