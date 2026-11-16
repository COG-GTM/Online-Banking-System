package com.userfront.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Parses user-supplied transfer amounts before any arithmetic is done with them.
 * Only plain decimals are accepted (no sign, no exponent), so a value such as
 * "1e1000000000" is rejected without ever being turned into a BigDecimal.
 */
public final class TransferAmount {

    public static final String MESSAGE =
            "Please enter an amount greater than 0 with up to 12 digits and at most 2 decimal places, e.g. 125.50.";

    private static final int MAX_LENGTH = 15;
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");

    private TransferAmount() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null) {
            throw new InvalidTransferAmountException(MESSAGE);
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_LENGTH || !PLAIN_DECIMAL.matcher(trimmed).matches()) {
            throw new InvalidTransferAmountException(MESSAGE);
        }
        BigDecimal amount = new BigDecimal(trimmed);
        if (amount.signum() <= 0 || amount.precision() > 14 || amount.scale() > 2) {
            throw new InvalidTransferAmountException(MESSAGE);
        }
        return amount;
    }
}
