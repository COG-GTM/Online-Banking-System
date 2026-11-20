package com.userfront.view;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;

public final class TransactionLog {

    public static final String PRIMARY = "Primary";
    public static final String SAVINGS = "Savings";

    private static final Comparator<TransactionLogEntry> NEWEST_FIRST = Comparator
            .comparingLong(TransactionLogEntry::getTimestamp)
            .thenComparing(TransactionLogEntry::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
            .reversed();

    private TransactionLog() {
    }

    public static List<TransactionLogEntry> fromPrimary(List<PrimaryTransaction> transactions) {
        if (transactions == null) {
            return Collections.emptyList();
        }
        return sortNewestFirst(transactions.stream()
                .map(t -> new TransactionLogEntry(t.getId(), t.getDate(), t.getDescription(), t.getType(),
                        t.getStatus(), t.getAmount(), t.getAvailableBalance(), PRIMARY))
                .collect(Collectors.toList()));
    }

    public static List<TransactionLogEntry> fromSavings(List<SavingsTransaction> transactions) {
        if (transactions == null) {
            return Collections.emptyList();
        }
        return sortNewestFirst(transactions.stream()
                .map(t -> new TransactionLogEntry(t.getId(), t.getDate(), t.getDescription(), t.getType(),
                        t.getStatus(), t.getAmount(), t.getAvailableBalance(), SAVINGS))
                .collect(Collectors.toList()));
    }

    static List<TransactionLogEntry> sortNewestFirst(List<TransactionLogEntry> entries) {
        List<TransactionLogEntry> sorted = new ArrayList<>(entries);
        sorted.sort(NEWEST_FIRST);
        return sorted;
    }

    public static String toCsv(List<TransactionLogEntry> entries) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        StringBuilder csv = new StringBuilder("Date,Description,Category,Type,Status,Amount,Available Balance\r\n");
        for (TransactionLogEntry entry : entries) {
            csv.append(entry.getDate() == null ? "" : dateFormat.format(entry.getDate())).append(',')
                    .append(textCell(entry.getDescription())).append(',')
                    .append(textCell(entry.getCategory().getLabel())).append(',')
                    .append(textCell(entry.getType())).append(',')
                    .append(textCell(entry.getStatus())).append(',')
                    .append(entry.getSignedAmount().toPlainString()).append(',')
                    .append(entry.getAvailableBalance() == null ? "" : entry.getAvailableBalance().toPlainString())
                    .append("\r\n");
        }
        return csv.toString();
    }

    private static String textCell(String value) {
        if (value == null) {
            return "";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
