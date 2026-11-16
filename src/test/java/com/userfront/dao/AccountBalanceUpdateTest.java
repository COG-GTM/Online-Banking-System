package com.userfront.dao;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

@RunWith(SpringRunner.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.globally_quoted_identifiers=true"
})
public class AccountBalanceUpdateTest {

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    public void debitNeverTakesBalanceBelowZero() {
        SavingsAccount account = savingsAccount("50.00");
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        assertEquals(0, (int) tx.execute(s -> savingsAccountDao.debitIfSufficient(account.getId(), new BigDecimal("50.01"))));
        assertEquals(1, (int) tx.execute(s -> savingsAccountDao.debitIfSufficient(account.getId(), new BigDecimal("50.00"))));
        assertEquals(0, savingsAccountDao.findById(account.getId()).get().getAccountBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void concurrentWithdrawalsAreAllSubtractedAndNeverOverdraw() throws Exception {
        PrimaryAccount account = primaryAccount("50.00");
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return tx.execute(s -> primaryAccountDao.debitIfSufficient(account.getId(), new BigDecimal("10.00")));
            }));
        }
        start.countDown();
        int debited = 0;
        for (Future<Integer> result : results) {
            debited += result.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(5, debited);
        assertEquals(0, primaryAccountDao.findById(account.getId()).get().getAccountBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void concurrentDepositsAreAllAdded() throws Exception {
        PrimaryAccount account = primaryAccount("0.00");
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return tx.execute(s -> primaryAccountDao.credit(account.getId(), new BigDecimal("2.50")));
            }));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertEquals(1, (int) result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(0, primaryAccountDao.findById(account.getId()).get().getAccountBalance().compareTo(new BigDecimal("25.00")));
    }

    private PrimaryAccount primaryAccount(String balance) {
        PrimaryAccount account = new PrimaryAccount();
        account.setAccountBalance(new BigDecimal(balance));
        return primaryAccountDao.save(account);
    }

    private SavingsAccount savingsAccount(String balance) {
        SavingsAccount account = new SavingsAccount();
        account.setAccountBalance(new BigDecimal(balance));
        return savingsAccountDao.save(account);
    }
}
