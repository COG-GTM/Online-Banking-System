package com.userfront.view;

public enum TransactionCategory {
    DEPOSIT("Deposit", "deposit", TransactionDirection.CREDIT),
    WITHDRAWAL("Withdrawal", "withdrawal", TransactionDirection.DEBIT),
    TRANSFER_IN("Transfer in", "transfer-in", TransactionDirection.CREDIT),
    TRANSFER_OUT("Transfer out", "transfer-out", TransactionDirection.DEBIT),
    PAYMENT("Payment", "payment", TransactionDirection.DEBIT),
    OTHER("Other", "other", TransactionDirection.UNKNOWN);

    private final String label;
    private final String style;
    private final TransactionDirection direction;

    TransactionCategory(String label, String style, TransactionDirection direction) {
        this.label = label;
        this.style = style;
        this.direction = direction;
    }

    public String getLabel() {
        return label;
    }

    public String getStyle() {
        return style;
    }

    public TransactionDirection getDirection() {
        return direction;
    }
}
