package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.userfront.dao.RoleDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.dto.SignupForm;
import com.userfront.service.UserServiceImpl.UserServiceImpl;

@RunWith(MockitoJUnitRunner.Silent.class)
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
    public void registerUserMapsFormOntoFreshUserWithUserRoleOnly() {
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        when(roleDao.findByName("ROLE_USER")).thenReturn(userRole);
        when(passwordEncoder.encode("correct-horse-battery")).thenReturn("[hashed]");
        when(accountService.createPrimaryAccount()).thenReturn(new PrimaryAccount());
        when(accountService.createSavingsAccount()).thenReturn(new SavingsAccount());
        when(userDao.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        SignupForm form = new SignupForm();
        form.setUsername("alice");
        form.setPassword("correct-horse-battery");
        form.setFirstName("Alice");
        form.setLastName("Smith");
        form.setEmail("alice@example.com");
        form.setPhone("555-123-4567");

        userService.registerUser(form);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        User saved = captor.getValue();
        assertNull(saved.getUserId());
        assertEquals("alice", saved.getUsername());
        assertEquals("[hashed]", saved.getPassword());
        assertNotEquals("correct-horse-battery", saved.getPassword());
        assertEquals("Alice", saved.getFirstName());
        assertEquals("Smith", saved.getLastName());
        assertEquals("alice@example.com", saved.getEmail());
        assertEquals("555-123-4567", saved.getPhone());
        assertTrue(saved.isEnabled());
        assertEquals(1, saved.getUserRoles().size());
        assertEquals("ROLE_USER", saved.getUserRoles().iterator().next().getRole().getName());
    }
}
