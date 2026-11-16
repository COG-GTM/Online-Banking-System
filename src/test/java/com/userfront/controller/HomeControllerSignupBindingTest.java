package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;
import com.userfront.service.UserService;

public class HomeControllerSignupBindingTest {

    private UserService userService;
    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        userService = mock(UserService.class);
        RoleDao roleDao = mock(RoleDao.class);
        when(roleDao.findByName("ROLE_USER")).thenReturn(new Role());
        when(userService.checkUserExists(anyString(), anyString())).thenReturn(false);

        HomeController controller = new HomeController();
        setField(controller, "userService", userService);
        setField(controller, "roleDao", roleDao);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void signupIgnoresNestedAndInternalUserFields() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("firstName", "Mallory")
                        .param("lastName", "M")
                        .param("phone", "555")
                        .param("email", "mallory@example.com")
                        .param("username", "mallory")
                        .param("password", "pw")
                        .param("recipientList[0].name", "x")
                        .param("enabled", "false")
                        .param("userId", "1"))
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(captor.capture(), any(Set.class));
        User user = captor.getValue();

        assertEquals("mallory", user.getUsername());
        assertEquals("mallory@example.com", user.getEmail());
        assertEquals("pw", user.getPassword());
        assertTrue(user.getRecipientList() == null || user.getRecipientList().isEmpty());
        assertTrue(user.isEnabled());
        assertEquals(null, user.getUserId());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
