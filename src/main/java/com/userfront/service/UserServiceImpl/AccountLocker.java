package com.userfront.service.UserServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

/**
 * Re-reads an account row inside the current transaction with a row-level
 * write lock (SELECT ... FOR UPDATE) so balance changes are serialized.
 * Must be called from within a transaction; always lock the primary account
 * before the savings account to keep lock ordering consistent.
 */
@Component
public class AccountLocker {

    @PersistenceContext
    private EntityManager entityManager;

    public PrimaryAccount lockPrimaryAccount(Long id) {
        PrimaryAccount account = entityManager.find(PrimaryAccount.class, id);
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }

    public SavingsAccount lockSavingsAccount(Long id) {
        SavingsAccount account = entityManager.find(SavingsAccount.class, id);
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }
}
