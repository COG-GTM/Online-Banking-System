package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;

import com.userfront.domain.User;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setSingleView(mock(View.class)).build();
    }

    @Test
    public void profilePostUpdatesAuthenticatedUserOnly() throws Exception {
        User alice = new User();
        alice.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(alice);

        MvcResult result = mockMvc.perform(post("/user/profile")
                .principal(() -> "alice")
                .param("username", "bob")
                .param("id", "2")
                .param("firstName", "Mallory")
                .param("lastName", "Mischief")
                .param("email", "mallory@example.com")
                .param("phone", "555-0100"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn();

        verify(userService).findByUsername("alice");
        verify(userService, never()).findByUsername("bob");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).saveUser(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("alice", savedUser.getUsername());
        assertEquals("Mallory", savedUser.getFirstName());
        assertEquals("Mischief", savedUser.getLastName());
        assertEquals("mallory@example.com", savedUser.getEmail());
        assertEquals("555-0100", savedUser.getPhone());
        assertSame(alice, result.getModelAndView().getModel().get("user"));
    }

    @Test
    public void profileGetLoadsAuthenticatedUser() throws Exception {
        User alice = new User();
        alice.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(alice);

        MvcResult result = mockMvc.perform(get("/user/profile").principal(() -> "alice"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andReturn();

        assertSame(alice, result.getModelAndView().getModel().get("user"));
    }
}
