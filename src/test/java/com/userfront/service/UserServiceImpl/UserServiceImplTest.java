package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.userfront.dao.RoleDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.domain.security.UserRole;
import com.userfront.service.AccountService;

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

    private Role roleUser;
    private PrimaryAccount primaryAccount;
    private SavingsAccount savingsAccount;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        roleUser = new Role();
        roleUser.setRoleId(1);
        roleUser.setName("ROLE_USER");
        primaryAccount = new PrimaryAccount();
        savingsAccount = new SavingsAccount();
        when(roleDao.findByName("ROLE_USER")).thenReturn(roleUser);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(accountService.createPrimaryAccount()).thenReturn(primaryAccount);
        when(accountService.createSavingsAccount()).thenReturn(savingsAccount);
        when(userDao.save(Mockito.any(User.class))).thenAnswer(inv -> inv.getArguments()[0]);
    }

    private User signupForm() {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("pw");
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setEmail("alice@example.com");
        user.setPhone("555");
        return user;
    }

    private User savedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userDao).save(captor.capture());
        return captor.getValue();
    }

    @Test
    public void assignsOnlyServerDefinedUserRole() {
        userService.createUser(signupForm());

        User saved = savedUser();
        assertEquals(1, saved.getUserRoles().size());
        UserRole userRole = saved.getUserRoles().iterator().next();
        assertSame(roleUser, userRole.getRole());
        assertSame(saved, userRole.getUser());
    }

    @Test
    public void ignoresClientSuppliedRoles() {
        Role admin = new Role();
        admin.setRoleId(2);
        admin.setName("ROLE_ADMIN");
        User form = signupForm();
        form.setUserRoles(new HashSet<>());
        form.getUserRoles().add(new UserRole(form, admin));

        userService.createUser(form);

        User saved = savedUser();
        assertEquals(1, saved.getUserRoles().size());
        assertEquals("ROLE_USER", saved.getUserRoles().iterator().next().getRole().getName());
        verify(roleDao, never()).save(Mockito.any(Role.class));
    }

    @Test
    public void ignoresClientSuppliedIdentityAndAccountState() {
        User form = signupForm();
        form.setUserId(1L);
        form.setEnabled(false);
        form.setPrimaryAccount(new PrimaryAccount());
        form.setSavingsAccount(new SavingsAccount());

        userService.createUser(form);

        User saved = savedUser();
        assertNull(saved.getUserId());
        assertTrue(saved.isEnabled());
        assertSame(primaryAccount, saved.getPrimaryAccount());
        assertSame(savingsAccount, saved.getSavingsAccount());
        assertEquals("alice", saved.getUsername());
        assertEquals("hashed", saved.getPassword());
        assertEquals("Alice", saved.getFirstName());
        assertEquals("Smith", saved.getLastName());
        assertEquals("alice@example.com", saved.getEmail());
        assertEquals("555", saved.getPhone());
    }

    @Test
    public void failsClosedWhenUserRoleIsMissing() {
        when(roleDao.findByName("ROLE_USER")).thenReturn(null);
        try {
            userService.createUser(signupForm());
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
        verify(userDao, never()).save(Mockito.any(User.class));
    }

    @Test
    public void existingUsernameIsNotModified() {
        User existing = new User();
        existing.setUsername("alice");
        when(userDao.findByUsername("alice")).thenReturn(existing);

        assertSame(existing, userService.createUser(signupForm()));
        verify(userDao, never()).save(Mockito.any(User.class));
        assertFalse(existing.getUserRoles().iterator().hasNext());
    }
}
