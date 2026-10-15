package com.userfront.service.UserServiceImpl;

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

@RunWith(MockitoJUnitRunner.Silent.class)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    public void checkUserExistsDetectsDuplicateEmailWithNewUsername() {
        when(userDao.findByEmail("taken@example.com")).thenReturn(new User());

        assertTrue(userService.checkUserExists("newuser", "taken@example.com"));
    }

    @Test
    public void checkUserExistsDetectsDuplicateUsername() {
        when(userDao.findByUsername("existing")).thenReturn(new User());

        assertTrue(userService.checkUserExists("existing", "fresh@example.com"));
    }

    @Test
    public void checkUserExistsIsFalseWhenUsernameAndEmailAreFree() {
        assertFalse(userService.checkUserExists("newuser", "fresh@example.com"));
    }

    @Test
    public void checkUserExistsDoesNotMatchEmailColumnAgainstUsername() {
        when(userDao.findByEmail("newuser")).thenReturn(new User());

        assertFalse(userService.checkUserExists("newuser", "fresh@example.com"));
    }
}
