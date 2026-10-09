package com.userfront.dao;

import java.math.BigDecimal;

import com.userfront.domain.SavingsAccount;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface SavingsAccountDao extends CrudRepository<SavingsAccount, Long> {

    SavingsAccount findByAccountNumber (int accountNumber);

    @Modifying
    @Query("update SavingsAccount a set a.accountBalance = a.accountBalance - :amount where a.id = :id and a.accountBalance >= :amount")
    int debitIfSufficientFunds(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("update SavingsAccount a set a.accountBalance = a.accountBalance + :amount where a.id = :id")
    int credit(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Query("select a.accountBalance from SavingsAccount a where a.id = :id")
    BigDecimal findBalanceById(@Param("id") Long id);
}
