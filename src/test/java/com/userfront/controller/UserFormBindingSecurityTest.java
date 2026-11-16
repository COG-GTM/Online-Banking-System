package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.service.UserService;

/**
 * A request parameter named after a form's model attribute ("user") must not load a stored
 * User. The conversion service below mimics Spring Data's DomainClassConverter, which the
 * running app registers for every repository domain type.
 */
public class UserFormBindingSecurityTest {

    private static final String VICTIM_HASH = "$2a$12$victimStoredBcryptHashValue";
    private static final String ATTACKER_HASH = "$2a$12$attackerChosenBcryptHashVal";

    private final Map<Long, User> storedUsers = new HashMap<>();
    private UserService userService;
    private User alice;
    private User mallory;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        alice = storedUser(7L, "alice", "alice@example.com", "Alice", "Victim", "555-0107", VICTIM_HASH);
        mallory = storedUser(8L, "mallory", "mallory@example.com", "Mallory", "Attacker", "555-0108", "$2a$12$malloryHash");

        userService = mock(UserService.class);
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("mallory")).thenReturn(mallory);
        when(userService.checkUserExists("alice", "alice@example.com")).thenReturn(true);
        when(userService.checkUsernameExists("alice")).thenReturn(true);
        when(userService.checkEmailExists("alice@example.com")).thenReturn(true);

        HomeController homeController = new HomeController();
        ReflectionTestUtils.setField(homeController, "userService", userService);
        ReflectionTestUtils.setField(homeController, "roleDao", mock(RoleDao.class));

        UserController userController = new UserController();
        ReflectionTestUtils.setField(userController, "userService", userService);

        FormattingConversionService conversionService = new FormattingConversionService();
        conversionService.addConverter(String.class, User.class, new Converter<String, User>() {
            @Override
            public User convert(String id) {
                return storedUsers.get(Long.valueOf(id));
            }
        });

        mockMvc = MockMvcBuilders.standaloneSetup(homeController, userController)
                .setConversionService(conversionService)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @Test
    public void signupWithUserParameterDoesNotRenderStoredRecord() throws Exception {
        MvcResult result = mockMvc.perform(post("/signup").param("user", "7")).andReturn();

        Object form = result.getModelAndView().getModel().get("user");
        assertNotNull(form);
        assertNotSame(alice, form);
        BeanWrapperImpl rendered = new BeanWrapperImpl(form);
        for (String field : new String[] {"firstName", "lastName", "phone", "email", "username", "password"}) {
            assertNull(field + " leaked from stored user", rendered.getPropertyValue(field));
        }
    }

    @Test
    public void signupWithUserParameterDoesNotOverwriteStoredPassword() throws Exception {
        mockMvc.perform(post("/signup").param("user", "7").param("password", ATTACKER_HASH));

        assertEquals(VICTIM_HASH, alice.getPassword());
        verify(userService, never()).createUser(same(alice), any());
        verify(userService, never()).saveUser(same(alice));
    }

    @Test
    public void signupCreatesNewUserFromFormFieldsOnly() throws Exception {
        mockMvc.perform(post("/signup")
                .param("user", "7")
                .param("userId", "7")
                .param("enabled", "false")
                .param("firstName", "New")
                .param("lastName", "Customer")
                .param("phone", "555-0199")
                .param("email", "new@example.com")
                .param("username", "newcustomer")
                .param("password", "s3cret"))
                .andExpect(view().name("redirect:/"));

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(created.capture(), any());
        User user = created.getValue();
        assertNotSame(alice, user);
        assertNull(user.getUserId());
        assertEquals(true, user.isEnabled());
        assertEquals("New", user.getFirstName());
        assertEquals("Customer", user.getLastName());
        assertEquals("555-0199", user.getPhone());
        assertEquals("new@example.com", user.getEmail());
        assertEquals("newcustomer", user.getUsername());
        assertEquals("s3cret", user.getPassword());
        assertEquals(VICTIM_HASH, alice.getPassword());
    }

    @Test
    public void signupRerenderForExistingUsernameDoesNotEchoPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/signup")
                .param("email", "alice@example.com")
                .param("username", "alice")
                .param("password", "typed-password"))
                .andExpect(view().name("signup"))
                .andReturn();

        Object form = result.getModelAndView().getModel().get("user");
        assertNull(new BeanWrapperImpl(form).getPropertyValue("password"));
    }

    @Test
    public void profileWithUserParameterDoesNotModifyAnotherCustomer() throws Exception {
        mockMvc.perform(post("/user/profile")
                .principal(principal("mallory"))
                .param("user", "7")
                .param("username", "alice")
                .param("password", ATTACKER_HASH)
                .param("firstName", "Pwned")
                .param("email", "pwned@example.com"));

        assertEquals(VICTIM_HASH, alice.getPassword());
        assertEquals("Alice", alice.getFirstName());
        assertEquals("alice@example.com", alice.getEmail());
        verify(userService, never()).saveUser(same(alice));
    }

    @Test
    public void profileUpdatesOnlyTheSignedInUser() throws Exception {
        mockMvc.perform(post("/user/profile")
                .principal(principal("mallory"))
                .param("username", "alice")
                .param("userId", "7")
                .param("firstName", "Mal")
                .param("lastName", "Lory")
                .param("email", "mal@example.com")
                .param("phone", "555-0000"))
                .andExpect(view().name("profile"));

        verify(userService).saveUser(same(mallory));
        assertEquals(Long.valueOf(8L), mallory.getUserId());
        assertEquals("mallory", mallory.getUsername());
        assertEquals("Mal", mallory.getFirstName());
        assertEquals("Lory", mallory.getLastName());
        assertEquals("mal@example.com", mallory.getEmail());
        assertEquals("555-0000", mallory.getPhone());
        assertNotEquals("Mal", alice.getFirstName());
        verify(userService, never()).findByUsername("alice");
    }

    private User storedUser(Long id, String username, String email, String first, String last, String phone, String hash) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        user.setPhone(phone);
        user.setPassword(hash);
        storedUsers.put(id, user);
        return user;
    }

    private static Principal principal(String name) {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
    }
}
