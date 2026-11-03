package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.AbstractView;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.dto.ProfileView;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    private static final String PASSWORD_HASH = "$2a$12$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQ";

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private User alice;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setViewResolvers((viewName, locale) -> new AbstractView() {
                    @Override
                    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                            HttpServletResponse response) {
                    }
                })
                .build();
        alice = user();
        when(userService.findByUsername("alice")).thenReturn(alice);
    }

    @Test
    public void profileModelExposesOnlyProfileView() throws Exception {
        Map<String, Object> model = mockMvc.perform(get("/user/profile").principal(principal("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn().getModelAndView().getModel();

        assertNoEntityInModel(model);
        ProfileView profile = (ProfileView) model.get("profile");
        assertEquals("alice", profile.getUsername());
        assertEquals("Alice", profile.getFirstName());
        assertEquals("Liddell", profile.getLastName());
        assertEquals("alice@example.com", profile.getEmail());
        assertEquals("555-0100", profile.getPhone());
        assertEquals(Integer.valueOf(11223145), profile.getPrimaryAccountNumber());
        assertEquals(Integer.valueOf(11223146), profile.getSavingsAccountNumber());
    }

    @Test
    public void profilePostModelExposesOnlyProfileView() throws Exception {
        Map<String, Object> model = mockMvc.perform(post("/user/profile").principal(principal("alice"))
                .param("username", "alice")
                .param("firstName", "Alicia")
                .param("lastName", "Liddell")
                .param("email", "alicia@example.com")
                .param("phone", "555-0199"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn().getModelAndView().getModel();

        assertNoEntityInModel(model);
        ProfileView profile = (ProfileView) model.get("profile");
        assertEquals("Alicia", profile.getFirstName());
        assertEquals("alicia@example.com", profile.getEmail());
        assertEquals("555-0199", profile.getPhone());
        assertEquals(PASSWORD_HASH, alice.getPassword());
    }

    @Test
    public void profileViewHasNoCredentialAccessors() {
        for (java.lang.reflect.Method method : ProfileView.class.getMethods()) {
            String name = method.getName().toLowerCase();
            assertFalse(name, name.contains("password"));
            assertFalse(name, name.contains("authorit"));
            assertFalse(name, name.contains("role"));
            assertFalse(name, name.contains("recipient"));
            assertFalse(name, name.contains("userid"));
        }
    }

    @Test
    public void profileViewToleratesMissingAccounts() {
        User user = user();
        user.setPrimaryAccount(null);
        user.setSavingsAccount(null);

        ProfileView profile = ProfileView.from(user);

        assertNull(profile.getPrimaryAccountNumber());
        assertNull(profile.getSavingsAccountNumber());
    }

    private void assertNoEntityInModel(Map<String, Object> model) {
        assertTrue(model.get("profile") instanceof ProfileView);
        assertFalse(model.containsKey("user"));
        for (Object value : model.values()) {
            assertFalse("persisted User entity placed on the model", value == alice);
            assertFalse(String.valueOf(value).contains(PASSWORD_HASH));
        }
    }

    private static Principal principal(final String name) {
        return new Principal() {
            @Override
            public String getName() {
                return name;
            }
        };
    }

    private static User user() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountNumber(11223145);
        SavingsAccount savings = new SavingsAccount();
        savings.setAccountNumber(11223146);

        User user = new User();
        user.setUserId(7L);
        user.setUsername("alice");
        user.setPassword(PASSWORD_HASH);
        user.setFirstName("Alice");
        user.setLastName("Liddell");
        user.setEmail("alice@example.com");
        user.setPhone("555-0100");
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        return user;
    }
}
