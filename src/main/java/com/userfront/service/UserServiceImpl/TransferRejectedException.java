package com.userfront.service.UserServiceImpl;

public class TransferRejectedException extends RuntimeException {

    private final String messageKey;

    public TransferRejectedException(String messageKey, String defaultMessage) {
        super(defaultMessage);
        this.messageKey = messageKey;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
