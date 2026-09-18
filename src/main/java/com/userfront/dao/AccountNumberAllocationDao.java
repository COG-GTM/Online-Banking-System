package com.userfront.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.userfront.domain.AccountNumberAllocation;

public interface AccountNumberAllocationDao extends JpaRepository<AccountNumberAllocation, Integer> {
}
