package com.userfront.service;

public class TransactionRejectedException extends RuntimeException {

    public TransactionRejectedException(String message) {
        super(message);
    }
}
