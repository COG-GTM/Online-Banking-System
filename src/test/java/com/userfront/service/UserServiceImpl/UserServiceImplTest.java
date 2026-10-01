package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void checkUserExistsDetectsExistingEmail() {
        when(userDao.findByEmail("taken@example.com")).thenReturn(new User());

        assertTrue(userService.checkUserExists("newuser", "taken@example.com"));
    }

    @Test
    public void checkUserExistsDetectsExistingUsername() {
        when(userDao.findByUsername("taken")).thenReturn(new User());

        assertTrue(userService.checkUserExists("taken", "new@example.com"));
    }

    @Test
    public void checkUserExistsIsFalseWhenNeitherExists() {
        assertFalse(userService.checkUserExists("newuser", "new@example.com"));
    }

    @Test
    public void checkUserExistsLooksUpTheEmailArgument() {
        userService.checkUserExists("newuser", "new@example.com");

        verify(userDao).findByEmail("new@example.com");
        verify(userDao, never()).findByEmail("newuser");
    }
}
