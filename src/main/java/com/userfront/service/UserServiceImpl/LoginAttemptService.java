package com.userfront.service.UserServiceImpl;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.UserDao;

/**
 * Counts failed logins with single-statement conditional UPDATEs so concurrent
 * attempts cannot overwrite each other's increments.
 */
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
        Date now = new Date();
        userDao.clearExpiredLockout(username, now);
        if (userDao.incrementFailedLoginAttempts(username) == 0) {
            return;
        }
        Date until = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(lockoutMinutes));
        if (userDao.lockIfThresholdReached(username, maxFailedAttempts, now, until) > 0) {
            LOG.warn("Account {} locked until {} after {} failed login attempts", username, until, maxFailedAttempts);
        }
    }

    @Transactional
    public void loginSucceeded(String username) {
        userDao.resetFailedLogins(username);
    }

    @Transactional(readOnly = true)
    public boolean isLocked(String username) {
        Date until = userDao.findLockoutUntilByUsername(username);
        return until != null && until.after(new Date());
    }
}
