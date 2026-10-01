package com.userfront.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.userfront.domain.User;

public interface UserDao extends CrudRepository<User, Long> {
	User findByUsername(String username);

    @Query("select distinct u from User u left join fetch u.userRoles ur left join fetch ur.role where u.username = :username")
    User findWithUserRolesByUsername(@Param("username") String username);

    User findByEmail(String email);

    @Query("select distinct u from User u left join fetch u.userRoles ur left join fetch ur.role left join fetch u.primaryAccount left join fetch u.savingsAccount")
    List<User> findAll();
}
