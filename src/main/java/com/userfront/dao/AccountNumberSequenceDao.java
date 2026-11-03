package com.userfront.dao;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.userfront.domain.AccountNumberSequence;

public interface AccountNumberSequenceDao extends JpaRepository<AccountNumberSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AccountNumberSequence s where s.name = :name")
    AccountNumberSequence findByNameForUpdate(@Param("name") String name);
}
