package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.UserService;

public class HomeControllerUserFrontTest {

    private UserService userService;
    private MockMvc mockMvc;
    private User alice;
    private User bob;

    @Before
    public void setUp() {
        userService = mock(UserService.class);
        HomeController controller = new HomeController();
        ReflectionTestUtils.setField(controller, "userService", userService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).setViewResolvers(viewResolver).build();

        alice = userWithAccounts(1L, "alice", 11111111);
        bob = userWithAccounts(2L, "bob", 22222222);
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("bob")).thenReturn(bob);
    }

    @Test
    public void dashboardIsLoadedForThePrincipalOnly() throws Exception {
        mockMvc.perform(get("/userFront").principal(principal("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("userFront"))
                .andExpect(model().attribute("primaryAccount", alice.getPrimaryAccount()))
                .andExpect(model().attribute("savingsAccount", alice.getSavingsAccount()))
                .andExpect(model().size(2));

        verify(userService).findByUsername("alice");
        verifyNoMoreInteractions(userService);
    }

    @Test
    public void callerSuppliedIdentifiersAreIgnored() throws Exception {
        mockMvc.perform(get("/userFront").principal(principal("alice"))
                        .param("username", "bob")
                        .param("userId", "2")
                        .param("primaryAccount.id", "2")
                        .param("primaryAccount.accountNumber", "22222222")
                        .param("savingsAccount.id", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("primaryAccount", alice.getPrimaryAccount()))
                .andExpect(model().attribute("savingsAccount", alice.getSavingsAccount()))
                .andExpect(model().size(2));

        verify(userService).findByUsername("alice");
        verify(userService, never()).findByUsername("bob");
        verifyNoMoreInteractions(userService);
        assertEquals(11111111, alice.getPrimaryAccount().getAccountNumber());
    }

    @Test
    public void nonGetMethodsAreRejected() throws Exception {
        mockMvc.perform(post("/userFront").principal(principal("alice")).param("username", "bob"))
                .andExpect(status().isMethodNotAllowed());

        verify(userService, never()).findByUsername(anyString());
    }

    @Test
    public void principalWithoutUserRecordIsDenied() throws Exception {
        assertAccessDenied(principal("mallory"));
        verify(userService).findByUsername("mallory");
    }

    @Test
    public void missingPrincipalIsDenied() throws Exception {
        assertAccessDenied(null);
        verify(userService, never()).findByUsername(anyString());
    }

    private void assertAccessDenied(Principal principal) throws Exception {
        try {
            if (principal == null) {
                mockMvc.perform(get("/userFront"));
            } else {
                mockMvc.perform(get("/userFront").principal(principal));
            }
            fail("expected AccessDeniedException");
        } catch (Exception e) {
            assertTrue(String.valueOf(e), e.getCause() instanceof AccessDeniedException);
        }
    }

    private static Principal principal(final String name) {
        return () -> name;
    }

    private static User userWithAccounts(Long id, String username, int accountNumber) {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setId(id);
        primary.setAccountNumber(accountNumber);
        SavingsAccount savings = new SavingsAccount();
        savings.setId(id);
        savings.setAccountNumber(accountNumber + 1);

        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        return user;
    }
}
