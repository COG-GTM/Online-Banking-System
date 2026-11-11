package com.userfront.view;

public enum TransactionDirection {
    CREDIT, DEBIT, UNKNOWN;

    public String getKey() {
        return name().toLowerCase();
    }
}
