package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.UserService;
import com.userfront.service.transfer.InsufficientFundsException;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(TransactionServiceImpl.class)
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class TransferRollbackIntegrationTest {

    @Autowired
    private TransactionServiceImpl service;
    @Autowired
    private PrimaryAccountDao primaryAccountDao;
    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @MockBean
    private PrimaryTransactionDao primaryTransactionDao;
    @MockBean
    private UserService userService;

    private PrimaryAccount primary;
    private SavingsAccount savings;

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        primary = primaryAccountDao.save(primary);
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
        savings = savingsAccountDao.save(savings);
    }

    @Test
    public void failedLedgerWriteRollsBackDebitAndCredit() {
        when(primaryTransactionDao.save(any(PrimaryTransaction.class)))
                .thenThrow(new IllegalStateException("ledger unavailable"));

        try {
            service.betweenAccountsTransfer("Primary", "Savings", "40", primary, savings);
            fail("expected ledger failure");
        } catch (IllegalStateException expected) {
        }

        assertBalances("100.00", "50.00");
    }

    @Test
    public void conditionalDebitRejectsOverdraftAgainstPersistedBalance() {
        primary.setAccountBalance(new BigDecimal("1000000.00"));

        try {
            service.betweenAccountsTransfer("Primary", "Savings", "100.01", primary, savings);
            fail("expected InsufficientFundsException");
        } catch (InsufficientFundsException expected) {
        }

        assertBalances("100.00", "50.00");
    }

    @Test
    public void successfulTransferMovesFunds() {
        service.betweenAccountsTransfer("Primary", "Savings", "40", primary, savings);

        assertBalances("60.00", "90.00");
    }

    private void assertBalances(String expectedPrimary, String expectedSavings) {
        assertEquals(new BigDecimal(expectedPrimary), primaryAccountDao.findBalanceById(primary.getId()));
        assertEquals(new BigDecimal(expectedSavings), savingsAccountDao.findBalanceById(savings.getId()));
    }
}
