package com.userfront.service;

public class InsufficientFundsException extends InvalidTransactionException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
