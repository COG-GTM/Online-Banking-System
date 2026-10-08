package com.userfront.dao;

import com.userfront.domain.PrimaryAccount;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface PrimaryAccountDao extends CrudRepository<PrimaryAccount,Long> {

    PrimaryAccount findByAccountNumber (int accountNumber);

    @Transactional
    @Modifying
    @Query("update PrimaryAccount a set a.accountBalance = a.accountBalance + :amount where a.id = :id")
    int creditBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Transactional
    @Modifying
    @Query("update PrimaryAccount a set a.accountBalance = a.accountBalance - :amount where a.id = :id and a.accountBalance >= :amount")
    int debitBalanceIfSufficient(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Query("select a.accountBalance from PrimaryAccount a where a.id = :id")
    BigDecimal findBalanceById(@Param("id") Long id);
}
