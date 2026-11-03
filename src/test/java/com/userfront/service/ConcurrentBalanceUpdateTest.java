package com.userfront.service;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConcurrentBalanceUpdateTest {

    private static final int REQUESTS = 20;
    private static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00");

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private UserDao userDao;

    private ExecutorService executor;
    private User user;

    @Before
    public void setUp() {
        executor = Executors.newFixedThreadPool(8);

        PrimaryAccount primaryAccount = accountService.createPrimaryAccount();
        primaryAccount.setAccountBalance(OPENING_BALANCE);
        primaryAccount = primaryAccountDao.save(primaryAccount);

        SavingsAccount savingsAccount = accountService.createSavingsAccount();
        savingsAccount.setAccountBalance(OPENING_BALANCE);
        savingsAccount = savingsAccountDao.save(savingsAccount);

        String username = "user-" + UUID.randomUUID();
        user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        user = userDao.save(user);
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void concurrentBetweenAccountsTransfersDoNotLoseUpdates() throws Exception {
        final PrimaryAccount stalePrimary = primaryAccountDao.findById(user.getPrimaryAccount().getId()).get();
        final SavingsAccount staleSavings = savingsAccountDao.findById(user.getSavingsAccount().getId()).get();

        runConcurrently(() -> {
            transactionService.betweenAccountsTransfer("Primary", "Savings", "10", stalePrimary, staleSavings);
            return null;
        });

        assertBalance("800.00", primaryAccountDao.findById(stalePrimary.getId()).get().getAccountBalance());
        assertBalance("1200.00", savingsAccountDao.findById(staleSavings.getId()).get().getAccountBalance());
    }

    @Test
    public void concurrentWithdrawalsDoNotLoseUpdates() throws Exception {
        final Principal principal = user::getUsername;

        runConcurrently(() -> {
            accountService.withdraw("Savings", 10, principal);
            return null;
        });

        assertBalance("800.00", savingsAccountDao.findById(user.getSavingsAccount().getId()).get().getAccountBalance());
    }

    private void runConcurrently(Callable<Void> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < REQUESTS; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                return request.call();
            }));
        }
        start.countDown();
        for (Future<Void> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
    }

    private static void assertBalance(String expected, BigDecimal actual) {
        assertEquals("expected balance " + expected + " but was " + actual, 0, new BigDecimal(expected).compareTo(actual));
    }
}
