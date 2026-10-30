package com.userfront.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.userfront.dao.RoleDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SignupForm;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserServiceImpl.UserServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplRegisterTest {

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
    public void registerUserMapsFormOntoFreshUserWithDefaultRoleAndNewAccounts() {
        Role roleUser = new Role();
        roleUser.setName("ROLE_USER");
        PrimaryAccount primaryAccount = new PrimaryAccount();
        SavingsAccount savingsAccount = new SavingsAccount();
        when(roleDao.findByName("ROLE_USER")).thenReturn(roleUser);
        when(passwordEncoder.encode("S3cure!Passw0rd")).thenReturn("encoded");
        when(accountService.createPrimaryAccount()).thenReturn(primaryAccount);
        when(accountService.createSavingsAccount()).thenReturn(savingsAccount);
        when(userDao.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SignupForm form = new SignupForm();
        form.setUsername("alice");
        form.setPassword("S3cure!Passw0rd");
        form.setFirstName("Alice");
        form.setLastName("Doe");
        form.setEmail("alice@example.com");
        form.setPhone("555-123-4567");

        User user = userService.registerUser(form);

        assertNull(user.getUserId());
        assertEquals("alice", user.getUsername());
        assertEquals("encoded", user.getPassword());
        assertEquals("Alice", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("555-123-4567", user.getPhone());
        assertTrue(user.isEnabled());
        assertSame(primaryAccount, user.getPrimaryAccount());
        assertSame(savingsAccount, user.getSavingsAccount());
        assertEquals(1, user.getUserRoles().size());
        UserRole userRole = user.getUserRoles().iterator().next();
        assertSame(roleUser, userRole.getRole());
        assertSame(user, userRole.getUser());
    }
}
