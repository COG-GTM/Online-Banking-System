package com.userfront.validation;

public class InvalidAmountException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
