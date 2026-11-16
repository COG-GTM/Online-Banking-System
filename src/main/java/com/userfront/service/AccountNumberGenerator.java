package com.userfront.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.userfront.dao.AccountNumberSequenceDao;
import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.AccountNumberSequence;

/**
 * Hands out account numbers from a counter row stored in the database, so numbers
 * survive restarts and stay unique across concurrent signups and app instances.
 * Primary and savings accounts share the same counter.
 */
@Service
public class AccountNumberGenerator {

    static final String SEQUENCE_NAME = "account_number";
    static final int FIRST_ACCOUNT_NUMBER = 11223146;

    private final AccountNumberSequenceDao sequenceDao;
    private final PrimaryAccountDao primaryAccountDao;
    private final SavingsAccountDao savingsAccountDao;
    private final TransactionTemplate transaction;
    private final TransactionTemplate newTransaction;

    @Autowired
    public AccountNumberGenerator(AccountNumberSequenceDao sequenceDao,
                                  PrimaryAccountDao primaryAccountDao,
                                  SavingsAccountDao savingsAccountDao,
                                  PlatformTransactionManager transactionManager) {
        this.sequenceDao = sequenceDao;
        this.primaryAccountDao = primaryAccountDao;
        this.savingsAccountDao = savingsAccountDao;
        this.transaction = new TransactionTemplate(transactionManager);
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int nextAccountNumber() {
        ensureSequenceExists();
        return transaction.execute(status -> {
            AccountNumberSequence sequence = sequenceDao.findByNameForUpdate(SEQUENCE_NAME);
            int value = sequence.getNextValue();
            if (value == Integer.MAX_VALUE) {
                throw new IllegalStateException("Account number space exhausted");
            }
            sequence.setNextValue(value + 1);
            sequenceDao.save(sequence);
            return value;
        });
    }

    private void ensureSequenceExists() {
        if (sequenceDao.countByName(SEQUENCE_NAME) > 0) {
            return;
        }
        try {
            newTransaction.execute(status -> {
                if (sequenceDao.countByName(SEQUENCE_NAME) == 0) {
                    sequenceDao.save(new AccountNumberSequence(SEQUENCE_NAME, firstFreeAccountNumber()));
                }
                return null;
            });
        } catch (DataAccessException | TransactionException e) {
            // Another signup created the row at the same moment; use theirs. Check in a fresh
            // transaction because the caller's snapshot may predate that row.
            if (newTransaction.execute(status -> sequenceDao.countByName(SEQUENCE_NAME)) == 0) {
                throw e;
            }
        }
    }

    private int firstFreeAccountNumber() {
        int highest = Math.max(maxOrZero(primaryAccountDao.findMaxAccountNumber()),
                maxOrZero(savingsAccountDao.findMaxAccountNumber()));
        return Math.max(FIRST_ACCOUNT_NUMBER, highest + 1);
    }

    private static int maxOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
