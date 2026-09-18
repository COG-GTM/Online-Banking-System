package com.userfront.validation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AmountValidator {

    private static final int MAX_SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private AmountValidator() {
    }

    public static BigDecimal parse(String amount) throws InvalidAmountException {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Amount must be a valid number, for example 25.00.");
        }

        if (parsedAmount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }

        if (parsedAmount.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidAmountException("Amount cannot have more than two decimal places.");
        }

        if (parsedAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount exceeds the maximum allowed per transaction.");
        }

        return parsedAmount.setScale(MAX_SCALE, RoundingMode.UNNECESSARY);
    }
}
