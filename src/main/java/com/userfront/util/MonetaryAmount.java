package com.userfront.util;

import java.math.BigDecimal;
import java.util.Optional;

public final class MonetaryAmount {

    public static final int SCALE = 2;

    public static final String ERROR_MESSAGE =
            "Please enter an amount greater than zero, with at most two decimal places.";

    private MonetaryAmount() {
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

        if (parsed.signum() <= 0 || parsed.scale() > SCALE) {
            return Optional.empty();
        }

        return Optional.of(parsed.setScale(SCALE));
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }
}
