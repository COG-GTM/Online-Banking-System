package com.userfront.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.userfront.service.InvalidTransactionException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidTransactionException.class)
    public String handleInvalidTransaction(InvalidTransactionException e, Model model) {
        LOG.warn("Rejected transaction: {}", e.getMessage());
        model.addAttribute("transactionError", e.getMessage());

        return "error";
    }
}
