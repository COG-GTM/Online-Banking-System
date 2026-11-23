package com.userfront.service;

public class InsufficientFundsException extends InvalidTransactionException {

    public InsufficientFundsException() {
        super("Insufficient funds: the amount exceeds the available balance.");
    }
}
