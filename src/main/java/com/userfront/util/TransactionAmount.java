package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.userfront.exception.InvalidAmountException;

public final class TransactionAmount {

    public static final int SCALE = 2;

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private TransactionAmount() {
    }

    public static BigDecimal parse(String amount) throws InvalidAmountException {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidAmountException("Please enter an amount.");
        }

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Please enter a valid amount, for example 25.00.");
        }

        if (parsedAmount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (parsedAmount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidAmountException("Amount cannot have more than two decimal places.");
        }
        if (parsedAmount.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount exceeds the maximum allowed for a single transaction.");
        }

        return parsedAmount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }
}
