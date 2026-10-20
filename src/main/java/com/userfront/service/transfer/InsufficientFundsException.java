package com.userfront.service.transfer;

public class InsufficientFundsException extends InvalidTransferException {

    public InsufficientFundsException(String message) {
        super(message);
    }
}
