package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class HomeControllerSignupTest {

	@Mock
	private UserService userService;

	@Mock
	private RoleDao roleDao;

	@InjectMocks
	private HomeController controller;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	public void signupOnlyBindsAllowedUserFields() throws Exception {
		Role role = new Role();
		role.setName("ROLE_USER");
		when(userService.checkUserExists(anyString(), anyString())).thenReturn(false);
		when(roleDao.findByName("ROLE_USER")).thenReturn(role);

		mockMvc.perform(post("/signup")
				.param("firstName", "Alice")
				.param("lastName", "Example")
				.param("phone", "555-0100")
				.param("email", "alice@example.com")
				.param("username", "alice")
				.param("password", "password")
				.param("userId", "7")
				.param("enabled", "false")
				.param("primaryAccount.id", "1"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userService).createUser(userCaptor.capture(), anySet());

		User createdUser = userCaptor.getValue();
		assertNull(createdUser.getUserId());
		assertTrue(createdUser.isEnabled());
		assertNull(createdUser.getPrimaryAccount());
		assertEquals("Alice", createdUser.getFirstName());
		assertEquals("Example", createdUser.getLastName());
		assertEquals("555-0100", createdUser.getPhone());
		assertEquals("alice@example.com", createdUser.getEmail());
		assertEquals("alice", createdUser.getUsername());
		assertEquals("password", createdUser.getPassword());
	}
}
