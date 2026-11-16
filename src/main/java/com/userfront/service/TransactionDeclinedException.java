package com.userfront.service;

public class TransactionDeclinedException extends RuntimeException {

    public TransactionDeclinedException(String message) {
        super(message);
    }
}
