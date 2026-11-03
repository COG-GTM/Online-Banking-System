package com.userfront.service.UserServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.userfront.dao.AccountNumberSequenceDao;
import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.AccountNumberSequence;

/**
 * Issues account numbers from a database-backed sequence row locked with SELECT ... FOR UPDATE,
 * so numbers stay unique across threads, application instances and restarts.
 */
@Service
public class AccountNumberGenerator {

    static final String SEQUENCE_NAME = "account_number";
    static final int INITIAL_ACCOUNT_NUMBER = 11223145;
    private static final int MAX_ALLOCATION_ATTEMPTS = 100;

    private final AccountNumberSequenceDao sequenceDao;
    private final PrimaryAccountDao primaryAccountDao;
    private final SavingsAccountDao savingsAccountDao;
    private final TransactionTemplate currentTransaction;
    private final TransactionTemplate newTransaction;

    @Autowired
    public AccountNumberGenerator(AccountNumberSequenceDao sequenceDao, PrimaryAccountDao primaryAccountDao,
                                  SavingsAccountDao savingsAccountDao, PlatformTransactionManager transactionManager) {
        this.sequenceDao = sequenceDao;
        this.primaryAccountDao = primaryAccountDao;
        this.savingsAccountDao = savingsAccountDao;
        this.currentTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSequenceRow() {
        try {
            newTransaction.execute(status -> {
                if (!sequenceDao.existsById(SEQUENCE_NAME)) {
                    sequenceDao.saveAndFlush(new AccountNumberSequence(SEQUENCE_NAME, highestIssuedAccountNumber()));
                }
                return null;
            });
        } catch (DataIntegrityViolationException | ConcurrencyFailureException e) {
            // Another transaction or application instance created the row first.
        }
    }

    /**
     * Joins the caller's transaction, so the sequence row stays locked until the new account is committed.
     */
    public int nextAccountNumber() {
        return currentTransaction.execute(status -> {
            // Locking a missing row in InnoDB takes a gap lock that deadlocks concurrent inserts,
            // so the row is created in its own transaction before it is locked.
            if (!sequenceDao.existsById(SEQUENCE_NAME)) {
                ensureSequenceRow();
            }

            // The locking read serializes allocators on InnoDB; the compare-and-set keeps
            // allocation atomic on engines without row locks (e.g. MyISAM).
            for (int attempt = 0; attempt < MAX_ALLOCATION_ATTEMPTS; attempt++) {
                Integer lastIssued = sequenceDao.lockLastIssued(SEQUENCE_NAME);
                if (lastIssued == null) {
                    throw new IllegalStateException("Account number sequence row is missing");
                }

                int next = Math.addExact(Math.max(lastIssued, highestIssuedAccountNumber()), 1);
                if (sequenceDao.compareAndSetLastIssued(SEQUENCE_NAME, lastIssued, next) == 1) {
                    return next;
                }
            }
            throw new IllegalStateException("Could not allocate an account number");
        });
    }

    private int highestIssuedAccountNumber() {
        int highest = INITIAL_ACCOUNT_NUMBER;
        Integer primaryMax = primaryAccountDao.findMaxAccountNumber();
        Integer savingsMax = savingsAccountDao.findMaxAccountNumber();
        if (primaryMax != null) {
            highest = Math.max(highest, primaryMax);
        }
        if (savingsMax != null) {
            highest = Math.max(highest, savingsMax);
        }
        return highest;
    }
}
