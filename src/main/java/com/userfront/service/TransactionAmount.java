package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Parses an amount typed into a money-movement form. Only plain positive decimals
 * with at most two fraction digits up to {@link #MAX} are accepted; the decimal
 * separator may be a point or a comma (French notation).
 */
public final class TransactionAmount {

    public static final BigDecimal MAX = new BigDecimal("1000000.00");

    private static final Pattern FORMAT = Pattern.compile("\\d{1,7}([.,]\\d{1,2})?");

    private TransactionAmount() {
    }

    public static BigDecimal parse(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("-")) {
            throw new TransactionDeclinedException("transaction.amount.positive", "Amount must be greater than zero.");
        }
        if (!FORMAT.matcher(text).matches()) {
            throw new TransactionDeclinedException("transaction.amount.format", "Enter an amount in dollars, for example 25 or 25.50, with no more than two decimal places.");
        }
        return requireValid(new BigDecimal(text.replace(',', '.')));
    }

    public static BigDecimal requireValid(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new TransactionDeclinedException("transaction.amount.positive", "Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new TransactionDeclinedException("transaction.amount.scale", "Amount cannot have more than two decimal places.");
        }
        if (amount.compareTo(MAX) > 0) {
            throw new TransactionDeclinedException("transaction.amount.max", "Amount cannot be more than $1,000,000.00.");
        }
        return amount.setScale(2);
    }
}
