package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class TransferAmount {

    private static final Pattern FORMAT = Pattern.compile("^\\d{1,12}(\\.\\d{1,2})?$");

    private TransferAmount() {
    }

    public static BigDecimal parse(String amount) {
        String trimmed = amount == null ? "" : amount.trim();
        if (!FORMAT.matcher(trimmed).matches()) {
            throw new InvalidTransferException("Please enter a positive amount with at most two decimal places.");
        }
        BigDecimal value = new BigDecimal(trimmed);
        if (value.signum() <= 0) {
            throw new InvalidTransferException("Please enter a positive amount with at most two decimal places.");
        }
        return value;
    }
}
