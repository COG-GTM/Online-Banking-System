package com.userfront.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecipientNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RecipientNotFoundException() {
        super("Recipient not found");
    }
}
