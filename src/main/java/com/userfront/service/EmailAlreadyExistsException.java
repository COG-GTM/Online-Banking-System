package com.userfront.service;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email already in use: " + email);
    }
}
