package com.userfront.util;

import java.math.BigDecimal;

import com.userfront.exception.InvalidAmountException;

public final class AmountParser {

    private AmountParser() {}

    public static BigDecimal parse(String amount) throws InvalidAmountException {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Amount must be a number, for example 25.00");
        }

        if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }

        if (parsedAmount.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount cannot have more than two decimal places.");
        }

        return parsedAmount.setScale(2);
    }
}
