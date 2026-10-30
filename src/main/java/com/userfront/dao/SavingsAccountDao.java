package com.userfront.dao;

import java.math.BigDecimal;

import com.userfront.domain.SavingsAccount;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by z00382545 on 10/21/16.
 */
public interface SavingsAccountDao extends CrudRepository<SavingsAccount, Long> {

    SavingsAccount findByAccountNumber (int accountNumber);

    @Modifying(clearAutomatically = true)
    @Query("update SavingsAccount a set a.accountBalance = a.accountBalance - :amount where a.id = :id and a.accountBalance >= :amount")
    int debitIfSufficient(@Param("id") Long id, @Param("amount") BigDecimal amount);

    @Modifying(clearAutomatically = true)
    @Query("update SavingsAccount a set a.accountBalance = a.accountBalance + :amount where a.id = :id")
    int credit(@Param("id") Long id, @Param("amount") BigDecimal amount);
}
