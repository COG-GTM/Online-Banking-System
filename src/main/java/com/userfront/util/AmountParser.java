package com.userfront.util;

import java.math.BigDecimal;
import java.util.Optional;

public final class AmountParser {

    private AmountParser() {}

    public static Optional<BigDecimal> parse(String amount) {
        if (amount == null) {
            return Optional.empty();
        }

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        if (parsedAmount.signum() <= 0) {
            return Optional.empty();
        }

        return Optional.of(parsedAmount);
    }
}
