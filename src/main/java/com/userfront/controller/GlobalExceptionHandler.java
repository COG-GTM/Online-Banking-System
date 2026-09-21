package com.userfront.controller;

import javax.persistence.EntityNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.userfront.service.InsufficientFundsException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({IllegalArgumentException.class, InsufficientFundsException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleInvalidRequest(RuntimeException exception) {
        LOG.warn("Rejected request: {}", exception.getMessage());

        return "error";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EntityNotFoundException exception) {
        LOG.warn("Resource not found: {}", exception.getMessage());

        return "error";
    }
}
