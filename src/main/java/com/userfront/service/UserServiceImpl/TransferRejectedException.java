package com.userfront.service.UserServiceImpl;

public class TransferRejectedException extends RuntimeException {

    public TransferRejectedException(String message) {
        super(message);
    }
}
