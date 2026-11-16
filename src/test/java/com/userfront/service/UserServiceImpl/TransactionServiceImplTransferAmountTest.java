package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.InvalidTransferAmountException;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class TransactionServiceImplTransferAmountTest {

    @Mock private UserService userService;
    @Mock private PrimaryTransactionDao primaryTransactionDao;
    @Mock private SavingsTransactionDao savingsTransactionDao;
    @Mock private PrimaryAccountDao primaryAccountDao;
    @Mock private SavingsAccountDao savingsAccountDao;
    @Mock private RecipientDao recipientDao;

    @InjectMocks private TransactionServiceImpl service;

    private PrimaryAccount primary;
    private SavingsAccount savings;
    private Recipient recipient;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
        recipient = new Recipient();
        recipient.setName("bob");
    }

    @Test(timeout = 2000)
    public void betweenAccountsRejectsHugeExponentBeforeTouchingBalances() throws Exception {
        for (String[] dir : new String[][] {{"Primary", "Savings"}, {"Savings", "Primary"}}) {
            for (String amount : new String[] {"1e1000000000", "1e100000000"}) {
                try {
                    service.betweenAccountsTransfer(dir[0], dir[1], amount, primary, savings);
                    fail("expected rejection of " + amount);
                } catch (InvalidTransferAmountException expected) {
                    // expected
                }
            }
        }
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verifyNoMoreInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }

    @Test(timeout = 2000)
    public void toSomeoneElseRejectsHugeExponentBeforeTouchingBalances() {
        for (String type : new String[] {"Primary", "Savings"}) {
            for (String amount : new String[] {"1e1000000000", "1e100000000"}) {
                try {
                    service.toSomeoneElseTransfer(recipient, type, amount, primary, savings);
                    fail("expected rejection of " + amount);
                } catch (InvalidTransferAmountException expected) {
                    // expected
                }
            }
        }
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
        verifyNoMoreInteractions(primaryAccountDao, savingsAccountDao, primaryTransactionDao, savingsTransactionDao);
    }

    @Test
    public void validAmountsStillTransfer() throws Exception {
        service.betweenAccountsTransfer("Primary", "Savings", "25.50", primary, savings);
        assertEquals(new BigDecimal("74.50"), primary.getAccountBalance());
        assertEquals(new BigDecimal("75.50"), savings.getAccountBalance());

        service.toSomeoneElseTransfer(recipient, "Savings", "10", primary, savings);
        assertEquals(new BigDecimal("65.50"), savings.getAccountBalance());
        verify(primaryTransactionDao).save(any(PrimaryTransaction.class));
    }
}
