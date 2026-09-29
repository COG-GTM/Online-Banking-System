package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountBalanceConcurrencyTest {

    private static final int THREADS = 20;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000");

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private PrimaryTransactionDao primaryTransactionDao;

    @Autowired
    private SavingsTransactionDao savingsTransactionDao;

    private User user;
    private Principal principal;

    @Before
    public void setUp() {
        PrimaryAccount primaryAccount = accountService.createPrimaryAccount();
        primaryAccount.setAccountBalance(OPENING_BALANCE);
        primaryAccountDao.save(primaryAccount);

        SavingsAccount savingsAccount = accountService.createSavingsAccount();
        savingsAccount.setAccountBalance(OPENING_BALANCE);
        savingsAccountDao.save(savingsAccount);

        user = new User();
        user.setUsername("race-" + UUID.randomUUID());
        user.setEmail(user.getUsername() + "@example.com");
        user.setPassword("secret");
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        userService.save(user);

        principal = user::getUsername;
    }

    @Test
    public void concurrentWithdrawalsNeverLoseAnUpdate() throws Exception {
        runConcurrently(THREADS, () -> accountService.withdraw("Primary", 10, principal));

        assertBalance(new BigDecimal("800"), reloadPrimary());
        assertEquals(THREADS, primaryTransactionCount());
    }

    @Test
    public void concurrentDepositsNeverLoseAnUpdate() throws Exception {
        runConcurrently(THREADS, () -> accountService.deposit("Savings", 25, principal));

        assertBalance(new BigDecimal("1500"), reloadSavings());
        assertEquals(THREADS, savingsTransactionCount());
    }

    @Test
    public void concurrentTransfersInBothDirectionsConserveMoney() throws Exception {
        List<Runnable> work = new ArrayList<>();
        for (int i = 0; i < THREADS / 2; i++) {
            work.add(() -> transfer("Primary", "Savings", "10"));
            work.add(() -> transfer("Savings", "Primary", "5"));
        }
        runConcurrently(work);

        assertBalance(new BigDecimal("950"), reloadPrimary());
        assertBalance(new BigDecimal("1050"), reloadSavings());
    }

    @Test
    public void mixedWithdrawalsAndTransfersOnSameAccountSerialize() throws Exception {
        List<Runnable> work = new ArrayList<>();
        for (int i = 0; i < THREADS / 2; i++) {
            work.add(() -> accountService.withdraw("Primary", 10, principal));
            work.add(() -> transfer("Primary", "Savings", "10"));
        }
        runConcurrently(work);

        assertBalance(new BigDecimal("800"), reloadPrimary());
        assertBalance(new BigDecimal("1100"), reloadSavings());
    }

    @Test
    public void invalidTransferLeavesBalancesUntouched() {
        try {
            transfer("Primary", "Primary", "10");
            throw new AssertionError("expected Invalid Transfer");
        } catch (RuntimeException expected) {
            assertTrue(expected.getCause().getMessage().contains("Invalid Transfer"));
        }

        assertBalance(OPENING_BALANCE, reloadPrimary());
        assertBalance(OPENING_BALANCE, reloadSavings());
    }

    private void transfer(String from, String to, String amount) {
        try {
            transactionService.betweenAccountsTransfer(from, to, amount,
                    userService.findByUsername(user.getUsername()).getPrimaryAccount(),
                    userService.findByUsername(user.getUsername()).getSavingsAccount());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void runConcurrently(int count, Runnable task) throws Exception {
        List<Runnable> work = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            work.add(task);
        }
        runConcurrently(work);
    }

    private void runConcurrently(List<Runnable> work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(work.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (Runnable task : work) {
            futures.add(pool.submit(() -> {
                start.await();
                task.run();
                return null;
            }));
        }
        start.countDown();
        pool.shutdown();
        assertTrue("workers did not finish", pool.awaitTermination(60, TimeUnit.SECONDS));
        for (Future<?> future : futures) {
            future.get();
        }
    }

    private PrimaryAccount reloadPrimary() {
        return primaryAccountDao.findById(user.getPrimaryAccount().getId()).get();
    }

    private SavingsAccount reloadSavings() {
        return savingsAccountDao.findById(user.getSavingsAccount().getId()).get();
    }

    private long primaryTransactionCount() {
        Long id = user.getPrimaryAccount().getId();
        return primaryTransactionDao.findAll().stream()
                .filter(t -> id.equals(t.getPrimaryAccount().getId()))
                .count();
    }

    private long savingsTransactionCount() {
        Long id = user.getSavingsAccount().getId();
        return savingsTransactionDao.findAll().stream()
                .filter(t -> id.equals(t.getSavingsAccount().getId()))
                .count();
    }

    private static void assertBalance(BigDecimal expected, PrimaryAccount account) {
        assertEquals(0, expected.compareTo(account.getAccountBalance()));
    }

    private static void assertBalance(BigDecimal expected, SavingsAccount account) {
        assertEquals(0, expected.compareTo(account.getAccountBalance()));
    }
}
