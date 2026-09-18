package com.userfront.util;

import java.math.BigDecimal;

import com.userfront.exception.InvalidAmountException;

public final class AmountParser {

    /**
     * Largest amount a single operation may move. Money columns are DECIMAL(19,2), so this
     * leaves room for a balance to accumulate without overflowing on the credited side.
     */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");

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

        if (parsedAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount cannot be greater than " + MAX_AMOUNT.toPlainString() + ".");
        }

        return parsedAmount.setScale(2);
    }

    /**
     * Guards the service layer against callers that did not go through {@link #parse(String)}:
     * an unvalidated negative amount would reverse the direction of a debit.
     */
    public static void requireValidAmount(BigDecimal amount) {
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.stripTrailingZeros().scale() > 2
                || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount);
        }
    }
}
