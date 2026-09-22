package com.userfront.exception;

import java.math.BigDecimal;

public class TransactionDeclinedException extends Exception {

    private static final long serialVersionUID = 1L;

    public TransactionDeclinedException(String message) {
        super(message);
    }

    public static TransactionDeclinedException insufficientFunds(String accountType, BigDecimal availableBalance, BigDecimal amount) {
        return new TransactionDeclinedException("Insufficient funds in the " + accountType + " account: available balance is "
                + availableBalance + " but " + amount + " was requested.");
    }

    public static TransactionDeclinedException invalidAmount(BigDecimal amount) {
        return new TransactionDeclinedException("The amount " + amount + " is not a valid transaction amount.");
    }
}
