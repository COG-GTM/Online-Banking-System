package com.userfront.service.UserServiceImpl;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;

@Service
public class LoginAttemptService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginAttemptService.class);

    @Autowired
    private UserDao userDao;

    @Value("${security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    @Transactional
    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        User user = userDao.findByUsername(username);
        if (user == null) {
            return;
        }
        user.recordFailedLogin(maxFailedAttempts, TimeUnit.MINUTES.toMillis(lockoutMinutes));
        userDao.save(user);
        if (!user.isAccountNonLocked()) {
            LOG.warn("Account {} locked until {} after {} failed login attempts",
                    username, user.getLockoutUntil(), user.getFailedLoginAttempts());
        }
    }

    @Transactional
    public void loginSucceeded(String username) {
        User user = userDao.findByUsername(username);
        if (user != null && (user.getFailedLoginAttempts() > 0 || user.getLockoutUntil() != null)) {
            user.resetFailedLogins();
            userDao.save(user);
        }
    }
}
