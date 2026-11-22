package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.User;
import com.userfront.service.UserService;

public class HomeControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
    }

    @Test
    public void signupBindsOnlyProfileFields() throws Exception {
        mockMvc.perform(post("/signup")
                .param("firstName", "Alice")
                .param("lastName", "Smith")
                .param("phone", "555")
                .param("email", "alice@example.com")
                .param("username", "alice")
                .param("password", "pw")
                .param("userId", "1")
                .param("enabled", "false")
                .param("userRoles[0].role.name", "ROLE_ADMIN")
                .param("primaryAccount.accountBalance", "1000000")
                .param("recipientList[0].name", "x"))
                .andExpect(redirectedUrl("/"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(captor.capture());
        User bound = captor.getValue();
        assertEquals("alice", bound.getUsername());
        assertEquals("pw", bound.getPassword());
        assertEquals("alice@example.com", bound.getEmail());
        assertNull(bound.getUserId());
        assertTrue(bound.isEnabled());
        assertTrue(bound.getUserRoles().isEmpty());
        assertNull(bound.getPrimaryAccount());
        assertNull(bound.getRecipientList());
    }
}
