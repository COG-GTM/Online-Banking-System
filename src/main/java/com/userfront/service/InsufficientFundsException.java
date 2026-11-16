package com.userfront.service;

public class InsufficientFundsException extends TransactionDeclinedException {

    public InsufficientFundsException() {
        super("transaction.insufficientFunds", "Insufficient funds: the account balance does not cover this amount.");
    }
}
