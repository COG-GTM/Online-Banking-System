package com.userfront.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.service.UserServiceImpl.UserServiceImpl;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UserServiceImplCheckUserExistsTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void existingEmailWithNewUsernameIsReportedAsExisting() {
        when(userDao.findByEmail("alice@example.com")).thenReturn(new User());

        assertTrue(userService.checkUserExists("fresh", "alice@example.com"));
    }

    @Test
    public void existingUsernameIsReportedAsExisting() {
        when(userDao.findByUsername("alice")).thenReturn(new User());

        assertTrue(userService.checkUserExists("alice", "fresh@example.com"));
    }

    @Test
    public void newUsernameAndEmailAreAvailable() {
        assertFalse(userService.checkUserExists("fresh", "fresh@example.com"));
    }
}
