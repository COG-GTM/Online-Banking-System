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
 * Re-reads an account row with SELECT ... FOR UPDATE so a balance check and the update that
 * follows cannot interleave with another request touching the same account.
 */
@Component
public class AccountLocks {

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
        T account = entityManager.find(type, id);
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }
}
