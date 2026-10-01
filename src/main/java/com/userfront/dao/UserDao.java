package com.userfront.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.userfront.domain.User;

public interface UserDao extends PagingAndSortingRepository<User, Long> {
	User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();

    @EntityGraph(attributePaths = {"primaryAccount", "savingsAccount"})
    Page<User> findAll(Pageable pageable);
}
