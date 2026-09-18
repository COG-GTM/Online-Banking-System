package com.userfront.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.userfront.exception.InvalidTransactionException;

public final class AmountValidator {

    private static final int MAX_SCALE = 2;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999999.99");

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

        if (parsed.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidTransactionException("The amount cannot be larger than " + MAX_AMOUNT.toPlainString() + ".");
        }

        if (parsed.stripTrailingZeros().scale() > MAX_SCALE) {
            throw new InvalidTransactionException("The amount cannot have more than two decimal places.");
        }

        try {
            return parsed.setScale(MAX_SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new InvalidTransactionException("The amount must be a number, for example 25.00.");
        }
    }
}
