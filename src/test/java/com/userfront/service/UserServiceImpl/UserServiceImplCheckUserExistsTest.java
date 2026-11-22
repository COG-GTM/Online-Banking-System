package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;

public class UserServiceImplCheckUserExistsTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void detectsExistingEmailWithNewUsername() {
        when(userDao.findByEmail("taken@example.com")).thenReturn(new User());

        assertTrue(userService.checkUserExists("fresh", "taken@example.com"));
    }

    @Test
    public void detectsExistingUsername() {
        when(userDao.findByUsername("taken")).thenReturn(new User());

        assertTrue(userService.checkUserExists("taken", "new@example.com"));
    }

    @Test
    public void allowsUnusedUsernameAndEmail() {
        assertFalse(userService.checkUserExists("fresh", "new@example.com"));
    }
}
