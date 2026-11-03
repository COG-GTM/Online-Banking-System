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
 * Re-reads account rows with a row-level write lock so balance updates are
 * computed from the latest committed balance, not a copy loaded earlier in
 * the request.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AccountLockSupport {

    @PersistenceContext
    private EntityManager entityManager;

    public PrimaryAccount lockPrimaryAccount(PrimaryAccount account) {
        return lock(PrimaryAccount.class, account.getId());
    }

    public SavingsAccount lockSavingsAccount(SavingsAccount account) {
        return lock(SavingsAccount.class, account.getId());
    }

    private <T> T lock(Class<T> type, Long id) {
        T managed = entityManager.find(type, id);
        if (managed == null) {
            throw new IllegalStateException(type.getSimpleName() + " " + id + " not found");
        }
        entityManager.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
        return managed;
    }
}
