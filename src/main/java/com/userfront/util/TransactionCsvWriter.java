package com.userfront.util;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;

public final class TransactionCsvWriter {

    static final String HEADER = "Post Date,Description,Type,Status,Amount,Available Balance";

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String LINE_END = "\r\n";

    private TransactionCsvWriter() {}

    public static void writePrimary(List<PrimaryTransaction> transactions, Writer writer) throws IOException {
        writer.write(HEADER + LINE_END);
        for (PrimaryTransaction t : transactions) {
            writeRow(writer, t.getDate(), t.getDescription(), t.getType(), t.getStatus(), t.getAmount(), t.getAvailableBalance());
        }
        writer.flush();
    }

    public static void writeSavings(List<SavingsTransaction> transactions, Writer writer) throws IOException {
        writer.write(HEADER + LINE_END);
        for (SavingsTransaction t : transactions) {
            writeRow(writer, t.getDate(), t.getDescription(), t.getType(), t.getStatus(), t.getAmount(), t.getAvailableBalance());
        }
        writer.flush();
    }

    private static void writeRow(Writer writer, Date date, String description, String type, String status,
                                 double amount, BigDecimal availableBalance) throws IOException {
        String dateText = date == null ? "" : new SimpleDateFormat(DATE_PATTERN).format(date);
        String balanceText = availableBalance == null ? "" : availableBalance.toPlainString();
        writer.write(String.join(",",
                escape(dateText),
                escape(description),
                escape(type),
                escape(status),
                BigDecimal.valueOf(amount).toPlainString(),
                balanceText));
        writer.write(LINE_END);
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = value;
        char first = safe.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            safe = "'" + safe;
        }
        if (safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
