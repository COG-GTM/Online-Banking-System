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
 * Re-reads an account row under a pessimistic write lock (SELECT ... FOR UPDATE) so that
 * balance changes within the caller's transaction are based on the latest committed balance.
 */
@Component
public class AccountLocker {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public PrimaryAccount lock(PrimaryAccount account) {
        return lock(PrimaryAccount.class, account.getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public SavingsAccount lock(SavingsAccount account) {
        return lock(SavingsAccount.class, account.getId());
    }

    private <T> T lock(Class<T> type, Long id) {
        T managed = entityManager.find(type, id);
        entityManager.refresh(managed, LockModeType.PESSIMISTIC_WRITE);
        return managed;
    }
}
