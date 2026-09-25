package com.userfront.dao;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

/**
 * Reloads an account row inside the current transaction while holding a write
 * lock on it, so a balance read cannot be stale or overwritten by a concurrent
 * balance change.
 */
@Repository
public class AccountLocker {

    @PersistenceContext
    private EntityManager entityManager;

    public PrimaryAccount lockPrimaryAccount(Long id) {
        PrimaryAccount primaryAccount = entityManager.find(PrimaryAccount.class, id);
        entityManager.refresh(primaryAccount, LockModeType.PESSIMISTIC_WRITE);

        return primaryAccount;
    }

    public SavingsAccount lockSavingsAccount(Long id) {
        SavingsAccount savingsAccount = entityManager.find(SavingsAccount.class, id);
        entityManager.refresh(savingsAccount, LockModeType.PESSIMISTIC_WRITE);

        return savingsAccount;
    }
}
