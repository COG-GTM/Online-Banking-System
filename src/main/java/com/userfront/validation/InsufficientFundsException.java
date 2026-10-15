package com.userfront.validation;

public class InsufficientFundsException extends InvalidAmountException {

    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
