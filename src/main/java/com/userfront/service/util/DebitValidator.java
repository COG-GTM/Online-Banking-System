package com.userfront.service.util;

import java.math.BigDecimal;

import com.userfront.exception.TransactionDeclinedException;

public final class DebitValidator {

    private DebitValidator() {
    }

    public static void validate(String accountType, BigDecimal availableBalance, BigDecimal amount) throws TransactionDeclinedException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw TransactionDeclinedException.invalidAmount(amount);
        }
        if (availableBalance == null || availableBalance.compareTo(amount) < 0) {
            throw TransactionDeclinedException.insufficientFunds(accountType, availableBalance, amount);
        }
    }
}
