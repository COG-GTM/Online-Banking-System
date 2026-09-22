package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountParser {

    public static final int SCALE = 2;

    private AmountParser() {
    }

    public static BigDecimal parse(String amount) {
        String trimmed = amount == null ? "" : amount.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Please enter a valid amount, for example 100.00.");
        }

        if (parsed.signum() <= 0) {
            throw new InvalidAmountException("Please enter an amount greater than zero.");
        }
        if (parsed.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidAmountException("Amounts cannot have more than two decimal places.");
        }

        return parsed.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
