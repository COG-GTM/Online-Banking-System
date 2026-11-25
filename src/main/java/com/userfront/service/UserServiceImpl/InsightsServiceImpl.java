package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.dto.Insights;
import com.userfront.service.InsightsService;

@Service
public class InsightsServiceImpl implements InsightsService {

    private static final String RECIPIENT_PREFIX = "Transfer to recipient ";
    private static final int TOP_RECIPIENTS = 5;

    private enum Kind { DEPOSIT, WITHDRAWAL, RECIPIENT, INTERNAL }

    private static final class Flow {
        final Date date;
        final Kind kind;
        final BigDecimal amount;
        final String recipient;

        Flow(Date date, String description, double amount) {
            this.date = date == null ? new Date(0) : date;
            this.amount = BigDecimal.valueOf(amount);
            String text = description == null ? "" : description;
            String lower = text.toLowerCase();
            if (text.startsWith(RECIPIENT_PREFIX)) {
                kind = Kind.RECIPIENT;
                recipient = text.substring(RECIPIENT_PREFIX.length());
            } else {
                recipient = null;
                kind = lower.startsWith("deposit") ? Kind.DEPOSIT
                        : lower.startsWith("withdraw") ? Kind.WITHDRAWAL
                        : Kind.INTERNAL;
            }
        }

        BigDecimal netWorthDelta() {
            switch (kind) {
                case DEPOSIT:
                    return amount;
                case WITHDRAWAL:
                case RECIPIENT:
                    return amount.negate();
                default:
                    return BigDecimal.ZERO;
            }
        }
    }

    @Override
    public Insights buildInsights(PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        List<Flow> flows = new ArrayList<>();
        BigDecimal primaryBalance = BigDecimal.ZERO;
        BigDecimal savingsBalance = BigDecimal.ZERO;

        if (primaryAccount != null) {
            primaryBalance = nonNull(primaryAccount.getAccountBalance());
            if (primaryAccount.getPrimaryTransactionList() != null) {
                for (PrimaryTransaction t : primaryAccount.getPrimaryTransactionList()) {
                    flows.add(new Flow(t.getDate(), t.getDescription(), t.getAmount()));
                }
            }
        }
        if (savingsAccount != null) {
            savingsBalance = nonNull(savingsAccount.getAccountBalance());
            if (savingsAccount.getSavingsTransactionList() != null) {
                for (SavingsTransaction t : savingsAccount.getSavingsTransactionList()) {
                    flows.add(new Flow(t.getDate(), t.getDescription(), t.getAmount()));
                }
            }
        }
        flows.sort(Comparator.comparing(f -> f.date));

        Insights insights = new Insights();
        BigDecimal netWorth = primaryBalance.add(savingsBalance);
        insights.setPrimaryBalance(primaryBalance);
        insights.setSavingsBalance(savingsBalance);
        insights.setNetWorth(netWorth);
        insights.setTransactionCount(flows.size());
        BigDecimal positiveSavings = savingsBalance.max(BigDecimal.ZERO);
        insights.setSavingsShare(percent(positiveSavings, positiveSavings.add(primaryBalance.max(BigDecimal.ZERO))));

        Map<Kind, BigDecimal> totals = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            totals.put(kind, BigDecimal.ZERO);
        }
        Map<String, BigDecimal> byRecipient = new LinkedHashMap<>();
        BigDecimal netChange = BigDecimal.ZERO;
        for (Flow flow : flows) {
            totals.merge(flow.kind, flow.amount, BigDecimal::add);
            if (flow.kind == Kind.RECIPIENT) {
                byRecipient.merge(flow.recipient, flow.amount, BigDecimal::add);
            }
            netChange = netChange.add(flow.netWorthDelta());
        }

        insights.setMoneyIn(totals.get(Kind.DEPOSIT));
        insights.setMoneyOut(totals.get(Kind.WITHDRAWAL).add(totals.get(Kind.RECIPIENT)));
        insights.setInternalTransfers(totals.get(Kind.INTERNAL));
        insights.setTrend(trend(flows, netWorth.subtract(netChange)));

        BigDecimal activity = totals.values().stream().filter(t -> t.signum() > 0).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Insights.Slice> breakdown = new ArrayList<>();
        addSlice(breakdown, "Deposits", totals.get(Kind.DEPOSIT), activity);
        addSlice(breakdown, "Withdrawals", totals.get(Kind.WITHDRAWAL), activity);
        addSlice(breakdown, "Sent to recipients", totals.get(Kind.RECIPIENT), activity);
        addSlice(breakdown, "Between accounts", totals.get(Kind.INTERNAL), activity);
        insights.setBreakdown(breakdown);

        BigDecimal sent = totals.get(Kind.RECIPIENT);
        insights.setTopRecipients(byRecipient.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(TOP_RECIPIENTS)
                .map(e -> new Insights.Slice(e.getKey(), e.getValue(), percent(e.getValue(), sent)))
                .collect(Collectors.toList()));

        return insights;
    }

    private static List<Insights.Point> trend(List<Flow> flows, BigDecimal opening) {
        SimpleDateFormat format = new SimpleDateFormat("MMM d HH:mm");
        BigDecimal running = opening;
        List<Insights.Point> trend = new ArrayList<>();
        trend.add(new Insights.Point("Start", running.doubleValue()));
        for (Flow flow : flows) {
            running = running.add(flow.netWorthDelta());
            trend.add(new Insights.Point(format.format(flow.date), running.doubleValue()));
        }
        return trend;
    }

    private static void addSlice(List<Insights.Slice> slices, String label, BigDecimal amount, BigDecimal total) {
        if (amount.signum() > 0) {
            slices.add(new Insights.Slice(label, amount, percent(amount, total)));
        }
    }

    private static int percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0) {
            return 0;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 0, RoundingMode.HALF_UP).intValue();
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
