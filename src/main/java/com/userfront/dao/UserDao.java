package com.userfront.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.userfront.domain.User;

public interface UserDao extends CrudRepository<User, Long> {
	User findByUsername(String username);
    User findByEmail(String email);
    List<User> findAll();

    @Query("select u.lockoutUntil from User u where u.username = :username")
    Date findLockoutUntilByUsername(@Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.failedLoginAttempts = 0, u.lockoutUntil = null "
            + "where u.username = :username and u.lockoutUntil is not null and u.lockoutUntil <= :now")
    int clearExpiredLockout(@Param("username") String username, @Param("now") Date now);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.failedLoginAttempts = coalesce(u.failedLoginAttempts, 0) + 1 "
            + "where u.username = :username")
    int incrementFailedLoginAttempts(@Param("username") String username);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.lockoutUntil = :until "
            + "where u.username = :username and u.failedLoginAttempts >= :maxAttempts "
            + "and (u.lockoutUntil is null or u.lockoutUntil <= :now)")
    int lockIfThresholdReached(@Param("username") String username, @Param("maxAttempts") int maxAttempts,
                               @Param("now") Date now, @Param("until") Date until);

    @Modifying(clearAutomatically = true)
    @Query("update User u set u.failedLoginAttempts = 0, u.lockoutUntil = null "
            + "where u.username = :username and (u.failedLoginAttempts > 0 or u.lockoutUntil is not null)")
    int resetFailedLogins(@Param("username") String username);
}
