package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.userfront.dao.AccountNumberSequenceDao;
import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.AccountService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountNumberGeneratorTest {

    @Autowired
    private AccountNumberGenerator generator;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountNumberSequenceDao sequenceDao;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Before
    public void resetDatabase() {
        primaryAccountDao.deleteAll();
        savingsAccountDao.deleteAll();
        sequenceDao.deleteAll();
    }

    @Test
    public void concurrentAccountCreationProducesUniqueNumbers() throws Exception {
        generator.ensureSequenceRow();
        assertAllUnique(runConcurrently(8, 25, () -> {
            List<Integer> numbers = new ArrayList<>();
            numbers.add(accountService.createPrimaryAccount().getAccountNumber());
            numbers.add(accountService.createSavingsAccount().getAccountNumber());
            return numbers;
        }), 8 * 25 * 2);
    }

    @Test
    public void concurrentSignupTransactionsProduceUniqueNumbersOnFirstUse() throws Exception {
        TransactionTemplate signupTransaction = new TransactionTemplate(transactionManager);
        assertAllUnique(runConcurrently(8, 10, () -> signupTransaction.execute(status -> {
            List<Integer> numbers = new ArrayList<>();
            numbers.add(accountService.createPrimaryAccount().getAccountNumber());
            numbers.add(accountService.createSavingsAccount().getAccountNumber());
            return numbers;
        })), 8 * 10 * 2);
    }

    private List<Integer> runConcurrently(int threads, int iterations, Callable<List<Integer>> work) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<List<Integer>>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    List<Integer> numbers = new ArrayList<>();
                    for (int i = 0; i < iterations; i++) {
                        numbers.addAll(work.call());
                    }
                    return numbers;
                }));
            }
            List<Integer> all = new ArrayList<>();
            for (Future<List<Integer>> future : futures) {
                all.addAll(future.get());
            }
            return all;
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void assertAllUnique(List<Integer> numbers, int expectedCount) {
        assertEquals(expectedCount, numbers.size());
        assertEquals(expectedCount, new HashSet<>(numbers).size());
        assertEquals(expectedCount, primaryAccountDao.count() + savingsAccountDao.count());
    }

    @Test
    public void continuesAfterHighestExistingNumberWhenSequenceRowIsMissing() {
        PrimaryAccount existing = new PrimaryAccount();
        existing.setAccountBalance(BigDecimal.ZERO);
        existing.setAccountNumber(AccountNumberGenerator.INITIAL_ACCOUNT_NUMBER + 500);
        primaryAccountDao.save(existing);

        int next = generator.nextAccountNumber();

        assertEquals(AccountNumberGenerator.INITIAL_ACCOUNT_NUMBER + 501, next);
        assertEquals(next, sequenceDao.findById(AccountNumberGenerator.SEQUENCE_NAME).get().getLastIssued());
    }

    @Test
    public void firstNumberMatchesLegacyStartingValue() {
        assertEquals(AccountNumberGenerator.INITIAL_ACCOUNT_NUMBER + 1, generator.nextAccountNumber());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void databaseRejectsDuplicateAccountNumbers() {
        int number = generator.nextAccountNumber();
        SavingsAccount first = new SavingsAccount();
        first.setAccountBalance(BigDecimal.ZERO);
        first.setAccountNumber(number);
        savingsAccountDao.save(first);

        SavingsAccount duplicate = new SavingsAccount();
        duplicate.setAccountBalance(BigDecimal.ZERO);
        duplicate.setAccountNumber(number);
        savingsAccountDao.save(duplicate);
        assertTrue("duplicate insert should fail", false);
    }
}
