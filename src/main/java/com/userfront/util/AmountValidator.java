package com.userfront.util;

import java.math.BigDecimal;

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
            throw new InvalidAmountException("Amount must be a number, for example 25.00.");
        }

        return validate(parsedAmount);
    }

    public static BigDecimal validate(BigDecimal amount) throws InvalidAmountException {
        if (amount == null) {
            throw new InvalidAmountException("Please enter an amount.");
        }
        if (amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.scale() > MAX_SCALE) {
            throw new InvalidAmountException("Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount exceeds the maximum allowed for a single transaction.");
        }

        return amount.setScale(MAX_SCALE);
    }
}
