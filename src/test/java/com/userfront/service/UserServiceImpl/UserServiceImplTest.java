package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
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
	public void createUserClearsSuppliedIdBeforeSaving() {
		User user = new User();
		user.setUserId(7L);
		user.setUsername("alice");
		user.setPassword("password");
		Role role = new Role();
		Set<UserRole> userRoles = Collections.singleton(new UserRole(user, role));

		when(userDao.findByUsername("alice")).thenReturn(null);
		when(passwordEncoder.encode("password")).thenReturn("enc");
		when(accountService.createPrimaryAccount()).thenReturn(new PrimaryAccount());
		when(accountService.createSavingsAccount()).thenReturn(new SavingsAccount());
		when(userDao.save(any(User.class))).thenAnswer(invocation -> (User) invocation.getArguments()[0]);

		userService.createUser(user, userRoles);

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userDao).save(userCaptor.capture());
		assertNull(userCaptor.getValue().getUserId());
		assertEquals("enc", userCaptor.getValue().getPassword());
	}

	@Test
	public void checkUserExistsChecksTheProvidedEmail() {
		when(userDao.findByUsername("newuser")).thenReturn(null);
		when(userDao.findByEmail("taken@x.com")).thenReturn(new User());

		assertTrue(userService.checkUserExists("newuser", "taken@x.com"));
	}
}
