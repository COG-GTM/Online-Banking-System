package com.userfront.service.UserServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @Mock
    private PrimaryTransactionDao primaryTransactionDao;

    @Mock
    private SavingsTransactionDao savingsTransactionDao;

    @Mock
    private RecipientDao recipientDao;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void rejectsNegativeTransferAmount() {
        PrimaryAccount primaryAccount = primaryAccount("100");
        SavingsAccount savingsAccount = savingsAccount("100");

        assertThatThrownBy(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "-50",
                primaryAccount, savingsAccount))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(primaryAccountDao, savingsAccountDao);
    }

    @Test
    void rejectsTransferExceedingBalance() {
        PrimaryAccount primaryAccount = primaryAccount("10");
        SavingsAccount savingsAccount = savingsAccount("0");

        assertThatThrownBy(() -> transactionService.betweenAccountsTransfer("Primary", "Savings", "50",
                primaryAccount, savingsAccount))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(primaryAccountDao, savingsAccountDao);
    }

    @Test
    void movesFundsBetweenOwnAccounts() throws Exception {
        PrimaryAccount primaryAccount = primaryAccount("100");
        SavingsAccount savingsAccount = savingsAccount("20");

        transactionService.betweenAccountsTransfer("Primary", "Savings", "30", primaryAccount, savingsAccount);

        assertThat(primaryAccount.getAccountBalance()).isEqualByComparingTo("70");
        assertThat(savingsAccount.getAccountBalance()).isEqualByComparingTo("50");
    }

    private static PrimaryAccount primaryAccount(String balance) {
        PrimaryAccount account = new PrimaryAccount();
        account.setAccountBalance(new BigDecimal(balance));
        return account;
    }

    private static SavingsAccount savingsAccount(String balance) {
        SavingsAccount account = new SavingsAccount();
        account.setAccountBalance(new BigDecimal(balance));
        return account;
    }
}
