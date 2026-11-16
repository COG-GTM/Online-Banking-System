package com.userfront.service;

public class RecipientLimitExceededException extends RuntimeException {

    public RecipientLimitExceededException(int limit) {
        super("Recipient limit of " + limit + " reached");
    }
}
