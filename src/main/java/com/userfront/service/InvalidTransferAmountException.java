package com.userfront.service;

public class InvalidTransferAmountException extends IllegalArgumentException {

    public InvalidTransferAmountException(String message) {
        super(message);
    }
}
