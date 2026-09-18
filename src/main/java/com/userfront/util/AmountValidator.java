package com.userfront.util;

import java.math.BigDecimal;
import java.util.Optional;

public final class AmountValidator {

    public static final String INVALID_AMOUNT_MESSAGE = "Please enter an amount greater than zero with at most two decimal places.";

    private static final int MAX_SCALE = 2;

    private AmountValidator() {
    }

    public static Optional<BigDecimal> parse(String amount) {
        if (amount == null) {
            return Optional.empty();
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        if (parsed.signum() <= 0 || parsed.stripTrailingZeros().scale() > MAX_SCALE) {
            return Optional.empty();
        }

        return Optional.of(parsed);
    }

    public static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new IllegalArgumentException(INVALID_AMOUNT_MESSAGE);
        }

        return amount;
    }
}
