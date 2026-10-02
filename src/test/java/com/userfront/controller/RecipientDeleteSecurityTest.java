package com.userfront.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.config.SecurityConfig;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(TransferController.class)
@Import(SecurityConfig.class)
public class RecipientDeleteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserSecurityService userSecurityService;

    private MockHttpSession session;

    private CsrfToken csrfToken;

    @Before
    public void setUp() {
        session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(new UsernamePasswordAuthenticationToken(
                        "alice", null, AuthorityUtils.createAuthorityList("ROLE_USER"))));

        HttpSessionCsrfTokenRepository tokenRepository = new HttpSessionCsrfTokenRepository();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        csrfToken = tokenRepository.generateToken(request);
        tokenRepository.saveToken(csrfToken, request, new MockHttpServletResponse());
    }

    @Test
    public void deleteWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").servletPath("/transfer/recipient/delete").session(session).param("recipientId", "42"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).deleteRecipientById(anyLong(), any(Principal.class));
    }

    @Test
    public void deleteWithCsrfTokenIsScopedToAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").servletPath("/transfer/recipient/delete").session(session)
                .param("recipientId", "42")
                .param(csrfToken.getParameterName(), csrfToken.getToken()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transfer/recipient"));

        verify(transactionService).deleteRecipientById(eq(42L), any(Principal.class));
    }
}
