package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.userfront.exception.InvalidTransactionException;

public final class AmountValidator {

    private static final int MAX_SCALE = 2;

    private AmountValidator() {
    }

    public static BigDecimal parse(String amount) throws InvalidTransactionException {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidTransactionException("Please specify an amount.");
        }

        BigDecimal parsed;
        try {
            parsed = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidTransactionException("The amount must be a number, for example 25.00.");
        }

        if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("The amount must be greater than zero.");
        }

        if (parsed.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidTransactionException("The amount cannot have more than two decimal places.");
        }

        return parsed.setScale(MAX_SCALE, RoundingMode.UNNECESSARY);
    }
}
