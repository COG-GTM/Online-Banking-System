package com.userfront.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionLogEntry {

    private static final String DISPLAY_DATE_PATTERN = "MMM d, yyyy h:mm a";

    private final Long id;
    private final Date date;
    private final String description;
    private final String type;
    private final String status;
    private final BigDecimal amount;
    private final BigDecimal availableBalance;
    private final TransactionCategory category;

    public TransactionLogEntry(Long id, Date date, String description, String type, String status,
                               double amount, BigDecimal availableBalance, String accountName) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.type = type;
        this.status = status;
        this.amount = BigDecimal.valueOf(amount).abs().setScale(2, RoundingMode.HALF_UP);
        this.availableBalance = availableBalance == null ? null : availableBalance.setScale(2, RoundingMode.HALF_UP);
        this.category = categorize(description, accountName);
    }

    static TransactionCategory categorize(String description, String accountName) {
        if (description == null) {
            return TransactionCategory.OTHER;
        }
        String text = description.trim().toLowerCase(Locale.US);
        if (text.startsWith("deposit")) {
            return TransactionCategory.DEPOSIT;
        }
        if (text.startsWith("withdraw")) {
            return TransactionCategory.WITHDRAWAL;
        }
        if (text.startsWith("transfer to recipient")) {
            return TransactionCategory.PAYMENT;
        }
        String betweenPrefix = "between account transfer from ";
        if (text.startsWith(betweenPrefix) && accountName != null) {
            String source = text.substring(betweenPrefix.length()).split("\\s+")[0];
            return source.equalsIgnoreCase(accountName) ? TransactionCategory.TRANSFER_OUT : TransactionCategory.TRANSFER_IN;
        }
        return TransactionCategory.OTHER;
    }

    public Long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public long getTimestamp() {
        return date == null ? 0L : date.getTime();
    }

    public String getFormattedDate() {
        return date == null ? "" : new SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US).format(date);
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public TransactionCategory getCategory() {
        return category;
    }

    public TransactionDirection getDirection() {
        return category.getDirection();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getSignedAmount() {
        return getDirection() == TransactionDirection.DEBIT ? amount.negate() : amount;
    }

    public String getFormattedAmount() {
        String formatted = formatCurrency(amount);
        switch (getDirection()) {
            case CREDIT:
                return "+" + formatted;
            case DEBIT:
                return "\u2212" + formatted;
            default:
                return formatted;
        }
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public String getFormattedAvailableBalance() {
        return availableBalance == null ? "" : formatCurrency(availableBalance);
    }

    private static String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(value);
    }
}
