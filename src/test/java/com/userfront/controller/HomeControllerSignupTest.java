package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.util.Set;

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
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class HomeControllerSignupTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleDao roleDao;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
        when(userService.checkUserExists(anyString(), anyString())).thenReturn(false);
        when(roleDao.findByName("ROLE_USER")).thenReturn(new Role());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void signupIgnoresUserIdAndOtherNonFormFields() throws Exception {
        mockMvc.perform(post("/signup")
                .param("userId", "1")
                .param("enabled", "false")
                .param("primaryAccount.id", "1")
                .param("savingsAccount.id", "1")
                .param("username", "evil")
                .param("password", "pw")
                .param("firstName", "Eve")
                .param("lastName", "Il")
                .param("email", "new@x.com")
                .param("phone", "555"))
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<Set<UserRole>> rolesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(userService).createUser(userCaptor.capture(), rolesCaptor.capture());

        User bound = userCaptor.getValue();
        assertNull(bound.getUserId());
        assertTrue(bound.isEnabled());
        assertNull(bound.getPrimaryAccount());
        assertNull(bound.getSavingsAccount());
        assertEquals("evil", bound.getUsername());
        assertEquals("pw", bound.getPassword());
        assertEquals("Eve", bound.getFirstName());
        assertEquals("Il", bound.getLastName());
        assertEquals("new@x.com", bound.getEmail());
        assertEquals("555", bound.getPhone());
        assertFalse(rolesCaptor.getValue().isEmpty());
    }
}
