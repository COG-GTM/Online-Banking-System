package com.userfront.service;

public class InvalidTransferException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    public InvalidTransferException(String message) {
        super(message);
    }
}
