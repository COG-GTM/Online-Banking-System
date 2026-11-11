package com.userfront.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;

public class TransactionLogTest {

    private static Date at(int day, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2026, Calendar.NOVEMBER, day, hour, minute, 0);
        return calendar.getTime();
    }

    private static PrimaryTransaction primary(long id, Date date, String description, double amount, String balance) {
        PrimaryTransaction transaction = new PrimaryTransaction(date, description, "Account", "Finished", amount,
                new BigDecimal(balance), null);
        transaction.setId(id);
        return transaction;
    }

    private static SavingsTransaction savings(long id, Date date, String description, double amount, String balance) {
        SavingsTransaction transaction = new SavingsTransaction(date, description, "Transfer", "Finished", amount,
                new BigDecimal(balance), null);
        transaction.setId(id);
        return transaction;
    }

    @Test
    public void categorizesEveryTransactionTheAppRecords() {
        assertEquals(TransactionCategory.DEPOSIT, TransactionLogEntry.categorize("Deposit to Primary Account", "Primary"));
        assertEquals(TransactionCategory.DEPOSIT, TransactionLogEntry.categorize("Deposit to savings Account", "Savings"));
        assertEquals(TransactionCategory.WITHDRAWAL, TransactionLogEntry.categorize("Withdraw from Primary Account", "Primary"));
        assertEquals(TransactionCategory.PAYMENT, TransactionLogEntry.categorize("Transfer to recipient John Smith", "Primary"));
        assertEquals(TransactionCategory.TRANSFER_OUT,
                TransactionLogEntry.categorize("Between account transfer from Primary to Savings", "Primary"));
        assertEquals(TransactionCategory.TRANSFER_IN,
                TransactionLogEntry.categorize("Between account transfer from Primary to Savings", "Savings"));
        assertEquals(TransactionCategory.TRANSFER_OUT,
                TransactionLogEntry.categorize("Between account transfer from Savings to Primary", "Savings"));
        assertEquals(TransactionCategory.OTHER, TransactionLogEntry.categorize("Interest adjustment", "Savings"));
        assertEquals(TransactionCategory.OTHER, TransactionLogEntry.categorize(null, "Savings"));
    }

    @Test
    public void formatsDatesAmountsAndBalancesForReading() {
        TransactionLogEntry deposit = TransactionLog.fromPrimary(Collections.singletonList(
                primary(1, at(11, 14, 5), "Deposit to Primary Account", 2500, "12500"))).get(0);
        TransactionLogEntry payment = TransactionLog.fromPrimary(Collections.singletonList(
                primary(2, at(11, 9, 30), "Transfer to recipient John Smith", 950.5, "1169.754"))).get(0);

        assertEquals("Nov 11, 2026 2:05 PM", deposit.getFormattedDate());
        assertEquals("+$2,500.00", deposit.getFormattedAmount());
        assertEquals("$12,500.00", deposit.getFormattedAvailableBalance());
        assertEquals(TransactionDirection.CREDIT, deposit.getDirection());
        assertEquals(new BigDecimal("2500.00"), deposit.getSignedAmount());

        assertEquals("\u2212$950.50", payment.getFormattedAmount());
        assertEquals("$1,169.75", payment.getFormattedAvailableBalance());
        assertEquals(TransactionDirection.DEBIT, payment.getDirection());
        assertEquals(new BigDecimal("-950.50"), payment.getSignedAmount());
    }

    @Test
    public void ordersNewestFirstAndBreaksTiesById() {
        Date sameMoment = at(10, 12, 0);
        List<TransactionLogEntry> log = TransactionLog.fromSavings(Arrays.asList(
                savings(1, at(1, 8, 0), "Deposit to savings Account", 100, "100"),
                savings(2, sameMoment, "Withdraw from savings Account", 10, "90"),
                savings(3, sameMoment, "Deposit to savings Account", 5, "95"),
                savings(4, at(5, 8, 0), "Between account transfer from Savings to Primary", 20, "70")));

        assertEquals(Arrays.asList(3L, 2L, 4L, 1L),
                Arrays.asList(log.get(0).getId(), log.get(1).getId(), log.get(2).getId(), log.get(3).getId()));
    }

    @Test
    public void handlesMissingTransactionLists() {
        assertTrue(TransactionLog.fromPrimary(null).isEmpty());
        assertTrue(TransactionLog.fromSavings(null).isEmpty());
    }

    @Test
    public void exportsCsvWithSignedAmountsAndSafeTextCells() {
        List<TransactionLogEntry> log = TransactionLog.fromPrimary(Arrays.asList(
                primary(1, at(11, 14, 5), "Deposit to Primary Account", 2500, "2500"),
                primary(2, at(11, 15, 0), "Transfer to recipient =HYPERLINK(\"x\"), Inc", 80.25, "2419.75")));

        String[] lines = TransactionLog.toCsv(log).split("\r\n");

        assertEquals("Date,Description,Category,Type,Status,Amount,Available Balance", lines[0]);
        assertEquals("2026-11-11 15:00:00,\"Transfer to recipient =HYPERLINK(\"\"x\"\"), Inc\",Payment,Account,Finished,-80.25,2419.75",
                lines[1]);
        assertEquals("2026-11-11 14:05:00,Deposit to Primary Account,Deposit,Account,Finished,2500.00,2500.00", lines[2]);
    }

    @Test
    public void neutralizesSpreadsheetFormulaPrefixes() {
        List<TransactionLogEntry> log = TransactionLog.fromPrimary(Collections.singletonList(
                primary(1, at(11, 14, 5), "=cmd|' /C calc'!A0", 1, "1")));

        String row = TransactionLog.toCsv(log).split("\r\n")[1];

        assertTrue(row, row.contains(",'=cmd|' /C calc'!A0,"));
    }
}
