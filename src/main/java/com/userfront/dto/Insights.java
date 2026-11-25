package com.userfront.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Insights {

    private BigDecimal primaryBalance = BigDecimal.ZERO;
    private BigDecimal savingsBalance = BigDecimal.ZERO;
    private BigDecimal netWorth = BigDecimal.ZERO;
    private BigDecimal moneyIn = BigDecimal.ZERO;
    private BigDecimal moneyOut = BigDecimal.ZERO;
    private BigDecimal internalTransfers = BigDecimal.ZERO;
    private int transactionCount;
    private int savingsShare;
    private List<Point> trend = new ArrayList<>();
    private List<Slice> breakdown = new ArrayList<>();
    private List<Slice> topRecipients = new ArrayList<>();

    public static class Point {
        private final String label;
        private final double value;

        public Point(String label, double value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public double getValue() {
            return value;
        }
    }

    public static class Slice {
        private final String label;
        private final BigDecimal amount;
        private final int percent;

        public Slice(String label, BigDecimal amount, int percent) {
            this.label = label;
            this.amount = amount;
            this.percent = percent;
        }

        public String getLabel() {
            return label;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public int getPercent() {
            return percent;
        }
    }

    public BigDecimal getPrimaryBalance() {
        return primaryBalance;
    }

    public void setPrimaryBalance(BigDecimal primaryBalance) {
        this.primaryBalance = primaryBalance;
    }

    public BigDecimal getSavingsBalance() {
        return savingsBalance;
    }

    public void setSavingsBalance(BigDecimal savingsBalance) {
        this.savingsBalance = savingsBalance;
    }

    public BigDecimal getNetWorth() {
        return netWorth;
    }

    public void setNetWorth(BigDecimal netWorth) {
        this.netWorth = netWorth;
    }

    public BigDecimal getMoneyIn() {
        return moneyIn;
    }

    public void setMoneyIn(BigDecimal moneyIn) {
        this.moneyIn = moneyIn;
    }

    public BigDecimal getMoneyOut() {
        return moneyOut;
    }

    public void setMoneyOut(BigDecimal moneyOut) {
        this.moneyOut = moneyOut;
    }

    public BigDecimal getInternalTransfers() {
        return internalTransfers;
    }

    public void setInternalTransfers(BigDecimal internalTransfers) {
        this.internalTransfers = internalTransfers;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public int getSavingsShare() {
        return savingsShare;
    }

    public void setSavingsShare(int savingsShare) {
        this.savingsShare = savingsShare;
    }

    public List<Point> getTrend() {
        return trend;
    }

    public void setTrend(List<Point> trend) {
        this.trend = trend;
    }

    public List<Slice> getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(List<Slice> breakdown) {
        this.breakdown = breakdown;
    }

    public List<Slice> getTopRecipients() {
        return topRecipients;
    }

    public void setTopRecipients(List<Slice> topRecipients) {
        this.topRecipients = topRecipients;
    }
}
