package com.userfront.service.UserServiceImpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.service.LoginAttemptService;

public class UserSecurityServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private UserSecurityService userSecurityService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        User alice = new User();
        alice.setUsername("alice");
        when(userDao.findByUsername("alice")).thenReturn(alice);
    }

    @Test
    public void accountIsReportedLockedWhileUsernameIsLocked() {
        when(loginAttemptService.isUsernameLocked("alice")).thenReturn(true);

        UserDetails details = userSecurityService.loadUserByUsername("alice");

        assertFalse(details.isAccountNonLocked());
    }

    @Test
    public void accountIsUnlockedOtherwise() {
        when(loginAttemptService.isUsernameLocked("alice")).thenReturn(false);

        UserDetails details = userSecurityService.loadUserByUsername("alice");

        assertTrue(details.isAccountNonLocked());
    }
}
