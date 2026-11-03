package com.userfront.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.userfront.domain.AccountNumberSequence;

public interface AccountNumberSequenceDao extends JpaRepository<AccountNumberSequence, String> {

    @Query(value = "SELECT last_issued FROM account_number_sequence WHERE name = :name FOR UPDATE", nativeQuery = true)
    Integer lockLastIssued(@Param("name") String name);

    @Modifying
    @Query("update AccountNumberSequence s set s.lastIssued = :next where s.name = :name and s.lastIssued = :current")
    int compareAndSetLastIssued(@Param("name") String name, @Param("current") int current, @Param("next") int next);
}
