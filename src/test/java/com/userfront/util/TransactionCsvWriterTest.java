package com.userfront.util;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;

public class TransactionCsvWriterTest {

    @Test
    public void writesHeaderOnlyForEmptyList() throws Exception {
        StringWriter out = new StringWriter();
        TransactionCsvWriter.writePrimary(Collections.<PrimaryTransaction>emptyList(), out);
        assertEquals(TransactionCsvWriter.HEADER + "\r\n", out.toString());
    }

    @Test
    public void writesPrimaryRows() throws Exception {
        PrimaryTransaction t = new PrimaryTransaction(
                new GregorianCalendar(2026, Calendar.NOVEMBER, 3, 9, 15, 0).getTime(),
                "Deposit to Primary Account", "Account", "Finished", 250.5, new BigDecimal("1250.50"), null);

        StringWriter out = new StringWriter();
        TransactionCsvWriter.writePrimary(Arrays.asList(t), out);

        assertEquals(TransactionCsvWriter.HEADER + "\r\n"
                + "2026-11-03 09:15:00,Deposit to Primary Account,Account,Finished,250.5,1250.50\r\n", out.toString());
    }

    @Test
    public void writesSavingsRowsWithMissingValues() throws Exception {
        SavingsTransaction t = new SavingsTransaction(null, null, "Transfer", "Finished", 10, null, null);

        StringWriter out = new StringWriter();
        TransactionCsvWriter.writeSavings(Arrays.asList(t), out);

        assertEquals(TransactionCsvWriter.HEADER + "\r\n" + ",,Transfer,Finished,10.0,\r\n", out.toString());
    }

    @Test
    public void quotesCommasQuotesAndNewlines() {
        assertEquals("\"Transfer to Smith, John\"", TransactionCsvWriter.escape("Transfer to Smith, John"));
        assertEquals("\"say \"\"hi\"\"\"", TransactionCsvWriter.escape("say \"hi\""));
        assertEquals("\"line1\nline2\"", TransactionCsvWriter.escape("line1\nline2"));
    }

    @Test
    public void neutralizesSpreadsheetFormulas() {
        assertEquals("'=1+2", TransactionCsvWriter.escape("=1+2"));
        assertEquals("\"'=HYPERLINK(\"\"x\"\")\"", TransactionCsvWriter.escape("=HYPERLINK(\"x\")"));
        assertEquals("'+1", TransactionCsvWriter.escape("+1"));
        assertEquals("'-1", TransactionCsvWriter.escape("-1"));
        assertEquals("'@SUM(A1)", TransactionCsvWriter.escape("@SUM(A1)"));
    }
}
