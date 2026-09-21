package com.userfront.service;

import java.math.BigDecimal;

import com.userfront.exception.InsufficientFundsException;
import com.userfront.exception.InvalidAmountException;

public final class DebitValidator {

    private DebitValidator() {
    }

    public static BigDecimal parseAmount(String amount) {
        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount);
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidAmountException("Amount is not a valid number.");
        }

        return validateAmount(parsedAmount);
    }

    public static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }

        return amount;
    }

    public static void validateSufficientFunds(BigDecimal accountBalance, BigDecimal amount) {
        BigDecimal balance = accountBalance == null ? BigDecimal.ZERO : accountBalance;
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds: the account balance does not cover this transaction.");
        }
    }
}
