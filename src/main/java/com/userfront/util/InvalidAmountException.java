package com.userfront.util;

public class InvalidAmountException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
