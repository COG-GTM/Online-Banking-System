package com.userfront.service.transfer;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class TransferAmount {

    private static final Pattern FORMAT = Pattern.compile("^\\d{1,12}(\\.\\d{1,2})?$");

    private TransferAmount() {
    }

    public static BigDecimal parse(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidTransferException("Amount must be a positive number with at most 2 decimal places.");
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) {
            throw new InvalidTransferException("Amount must be greater than zero.");
        }
        return amount;
    }
}
