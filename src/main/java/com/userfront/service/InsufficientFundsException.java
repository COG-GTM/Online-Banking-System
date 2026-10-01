package com.userfront.service;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String accountType, BigDecimal availableBalance, BigDecimal requestedAmount) {
        super("Insufficient funds in your " + accountType + " account: available $"
                + availableBalance.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + ", requested $" + requestedAmount.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + ".");
    }
}
