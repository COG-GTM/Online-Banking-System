package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    private User user(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setFirstName(username + "-first");
        u.setLastName(username + "-last");
        u.setEmail(email);
        u.setPhone("555-1111");
        return u;
    }

    @Test
    public void updateProfileOnlyModifiesTheGivenUserAndIgnoresSubmittedUsername() {
        User attacker = user("attacker", "attacker@example.com");
        when(userDao.findByUsername("attacker")).thenReturn(attacker);
        when(userDao.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User form = user("victim", "new@example.com");
        form.setFirstName("Changed");

        User result = userService.updateProfile("attacker", form);

        verify(userDao, never()).findByUsername("victim");
        verify(userDao).save(attacker);
        assertEquals("attacker", result.getUsername());
        assertEquals("Changed", result.getFirstName());
        assertEquals("new@example.com", result.getEmail());
    }

    @Test(expected = UsernameNotFoundException.class)
    public void updateProfileFailsForUnknownUser() {
        userService.updateProfile("ghost", user("victim", "x@example.com"));
    }

    @Test
    public void emailTakenOnlyWhenOwnedByDifferentUser() {
        when(userDao.findByEmail("victim@example.com")).thenReturn(user("victim", "victim@example.com"));
        when(userDao.findByEmail("attacker@example.com")).thenReturn(user("attacker", "attacker@example.com"));

        assertTrue(userService.isEmailTakenByAnotherUser("victim@example.com", "attacker"));
        assertFalse(userService.isEmailTakenByAnotherUser("attacker@example.com", "attacker"));
        assertFalse(userService.isEmailTakenByAnotherUser("free@example.com", "attacker"));
    }
}
