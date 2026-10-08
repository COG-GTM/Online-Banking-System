package com.userfront.service;

public class InsufficientFundsException extends InvalidTransferException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException() {
        super("Insufficient funds in the source account.");
    }
}
