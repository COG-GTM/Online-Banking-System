package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

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
import com.userfront.domain.security.UserRole;
import com.userfront.service.AccountService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UserServiceImplCreateUserTest {

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
    public void createUserNeverPersistsCallerSuppliedIdentityOrRelations() {
        User attackerInput = new User();
        attackerInput.setUserId(1L);
        attackerInput.setEnabled(false);
        attackerInput.setUsername("evil");
        attackerInput.setPassword("pw");
        attackerInput.setEmail("new@x.com");
        attackerInput.getUserRoles().add(new UserRole(attackerInput, new Role()));

        Set<UserRole> userRoles = new HashSet<>();
        UserRole userRole = new UserRole(attackerInput, new Role());
        userRoles.add(userRole);

        PrimaryAccount primaryAccount = new PrimaryAccount();
        SavingsAccount savingsAccount = new SavingsAccount();
        when(userDao.findByUsername("evil")).thenReturn(null);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        when(accountService.createPrimaryAccount()).thenReturn(primaryAccount);
        when(accountService.createSavingsAccount()).thenReturn(savingsAccount);
        when(userDao.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser(attackerInput, userRoles);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(saved.capture());
        User persisted = saved.getValue();

        assertNotSame(attackerInput, persisted);
        assertNull(persisted.getUserId());
        assertTrue(persisted.isEnabled());
        assertEquals("evil", persisted.getUsername());
        assertEquals("encoded", persisted.getPassword());
        assertEquals("new@x.com", persisted.getEmail());
        assertSame(primaryAccount, persisted.getPrimaryAccount());
        assertSame(savingsAccount, persisted.getSavingsAccount());
        assertEquals(1, persisted.getUserRoles().size());
        assertTrue(persisted.getUserRoles().contains(userRole));
        assertSame(persisted, userRole.getUser());
    }
}
