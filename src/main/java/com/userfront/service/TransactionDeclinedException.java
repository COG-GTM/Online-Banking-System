package com.userfront.service;

public class TransactionDeclinedException extends RuntimeException {

    private final String messageKey;

    public TransactionDeclinedException(String messageKey, String defaultMessage) {
        super(defaultMessage);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
