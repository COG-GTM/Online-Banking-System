package com.userfront.dao;

import javax.persistence.LockModeType;

import com.userfront.domain.SavingsAccount;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface SavingsAccountDao extends CrudRepository<SavingsAccount, Long> {

    SavingsAccount findByAccountNumber (int accountNumber);

    boolean existsByAccountNumber(int accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SavingsAccount a where a.id = :id")
    SavingsAccount findLockedById(@Param("id") Long id);
}
