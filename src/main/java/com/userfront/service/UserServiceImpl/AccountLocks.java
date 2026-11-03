package com.userfront.service.UserServiceImpl;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

/**
 * Re-reads an account row with SELECT ... FOR UPDATE inside the caller's transaction,
 * so balance changes are computed from the latest committed balance and concurrent
 * updates to the same account are serialized. Callers that lock both accounts must
 * lock the primary account first.
 */
@Component
public class AccountLocks {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public PrimaryAccount lockPrimary(Long id) {
        return lock(PrimaryAccount.class, id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public SavingsAccount lockSavings(Long id) {
        return lock(SavingsAccount.class, id);
    }

    private <T> T lock(Class<T> type, Long id) {
        T account = entityManager.find(type, id);
        if (account == null) {
            throw new IllegalStateException(type.getSimpleName() + " " + id + " not found");
        }
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }
}
