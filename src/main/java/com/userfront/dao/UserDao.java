package com.userfront.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.userfront.domain.User;

public interface UserDao extends CrudRepository<User, Long> {
	User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();

    @Query(value = "select u from User u left join fetch u.primaryAccount left join fetch u.savingsAccount",
            countQuery = "select count(u) from User u")
    Page<User> findAllWithAccounts(Pageable pageable);
}
