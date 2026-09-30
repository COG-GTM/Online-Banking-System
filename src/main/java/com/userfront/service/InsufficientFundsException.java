package com.userfront.service;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String accountType, BigDecimal balance, BigDecimal amount) {
        super("Insufficient funds in " + accountType + " account: available balance is $"
                + balance.toPlainString() + ", requested $" + amount.toPlainString() + ".");
    }
}
