package com.userfront.dao;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.SavingsAccount;

public interface SavingsAccountDao extends CrudRepository<SavingsAccount, Long> {

    SavingsAccount findByAccountNumber(int accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SavingsAccount> findWithLockById(Long id);
}
