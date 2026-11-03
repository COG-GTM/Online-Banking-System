package com.userfront.service;

public class InvalidAmountException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
