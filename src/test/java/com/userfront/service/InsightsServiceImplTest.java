package com.userfront.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.dto.Insights;
import com.userfront.service.UserServiceImpl.InsightsServiceImpl;

class InsightsServiceImplTest {

    private final InsightsServiceImpl service = new InsightsServiceImpl();

    @Test
    void summarisesFlowsTrendAndRecipients() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("5050"));
        List<PrimaryTransaction> primaryTx = new ArrayList<>();
        primaryTx.add(new PrimaryTransaction(at(1), "Deposit to Primary Account", "Account", "Finished", 5000, null, primary));
        primaryTx.add(new PrimaryTransaction(at(3), "Withdraw from Primary Account", "Account", "Finished", 250, null, primary));
        primaryTx.add(new PrimaryTransaction(at(5), "Transfer to recipient John Smith", "Transfer", "Finished", 1200, null, primary));
        primary.setPrimaryTransactionList(primaryTx);

        SavingsAccount savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("10500"));
        List<SavingsTransaction> savingsTx = new ArrayList<>();
        savingsTx.add(new SavingsTransaction(at(2), "Deposit to savings Account", "Account", "Finished", 12000, null, savings));
        savingsTx.add(new SavingsTransaction(at(4), "Between account transfer from Savings to Primary", "Transfer", "Finished", 1500, null, savings));
        savings.setSavingsTransactionList(savingsTx);

        Insights insights = service.buildInsights(primary, savings);

        assertThat(insights.getNetWorth()).isEqualByComparingTo("15550");
        assertThat(insights.getMoneyIn()).isEqualByComparingTo("17000");
        assertThat(insights.getMoneyOut()).isEqualByComparingTo("1450");
        assertThat(insights.getInternalTransfers()).isEqualByComparingTo("1500");
        assertThat(insights.getTransactionCount()).isEqualTo(5);
        assertThat(insights.getSavingsShare()).isEqualTo(68);

        assertThat(insights.getTrend()).extracting(Insights.Point::getValue)
                .containsExactly(0.0, 5000.0, 17000.0, 16750.0, 16750.0, 15550.0);

        assertThat(insights.getBreakdown()).extracting(Insights.Slice::getLabel)
                .containsExactly("Deposits", "Withdrawals", "Sent to recipients", "Between accounts");
        assertThat(insights.getTopRecipients()).hasSize(1);
        assertThat(insights.getTopRecipients().get(0).getLabel()).isEqualTo("John Smith");
        assertThat(insights.getTopRecipients().get(0).getPercent()).isEqualTo(100);
    }

    @Test
    void handlesAccountsWithoutActivity() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountBalance(BigDecimal.ZERO);
        SavingsAccount savings = new SavingsAccount();
        savings.setAccountBalance(BigDecimal.ZERO);

        Insights insights = service.buildInsights(primary, savings);

        assertThat(insights.getNetWorth()).isEqualByComparingTo("0");
        assertThat(insights.getSavingsShare()).isZero();
        assertThat(insights.getTrend()).hasSize(1);
        assertThat(insights.getBreakdown()).isEmpty();
        assertThat(insights.getTopRecipients()).isEmpty();
    }

    @Test
    void keepsPercentagesInRangeForOverdraftsAndNegativeAmounts() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("-50"));
        List<PrimaryTransaction> primaryTx = new ArrayList<>();
        primaryTx.add(new PrimaryTransaction(at(1), "Deposit to Primary Account", "Account", "Finished", 300, null, primary));
        primaryTx.add(new PrimaryTransaction(at(2), "Withdraw from Primary Account", "Account", "Finished", -100, null, primary));
        primary.setPrimaryTransactionList(primaryTx);
        SavingsAccount savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("100"));

        Insights insights = service.buildInsights(primary, savings);

        assertThat(insights.getSavingsShare()).isEqualTo(100);
        assertThat(insights.getBreakdown()).hasSize(1);
        assertThat(insights.getBreakdown().get(0).getPercent()).isEqualTo(100);
    }

    private static Date at(int minute) {
        return new Date(1_700_000_000_000L + minute * 60_000L);
    }
}
