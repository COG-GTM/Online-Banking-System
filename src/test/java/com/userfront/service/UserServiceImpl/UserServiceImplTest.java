package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.userfront.dao.RoleDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.domain.security.UserRole;
import com.userfront.service.AccountService;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @Mock
    private RoleDao roleDao;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AccountService accountService;

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
    public void checkUserExistsLooksUpEmailByEmailNotUsername() {
        assertFalse(userService.checkUserExists("newuser", "new@example.com"));
        verify(userDao).findByEmail("new@example.com");
        verify(userDao, never()).findByEmail("newuser");
    }

    @Test
    public void createUserDoesNotSaveWhenEmailAlreadyExists() {
        User user = new User();
        user.setUsername("newuser");
        user.setEmail("taken@example.com");
        user.setPassword("secret");
        when(userDao.findByEmail("taken@example.com")).thenReturn(new User());

        assertNull(userService.createUser(user, new HashSet<UserRole>()));
        verify(userDao, never()).save(any(User.class));
    }
}
