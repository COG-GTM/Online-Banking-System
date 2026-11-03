package com.userfront.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.controller.TransferController;
import com.userfront.dao.UserDao;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RecipientDeleteCsrfTest.TestConfig.class)
public class RecipientDeleteCsrfTest {

    private static final String TOKEN_ATTRIBUTE = HttpSessionCsrfTokenRepository.class.getName() + ".CSRF_TOKEN";

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, TransferController.class})
    static class TestConfig {

        @Bean
        public UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        public TransactionService transactionService() {
            return mock(TransactionService.class);
        }

        @Bean
        public UserDao userDao() {
            return mock(UserDao.class);
        }

        @Bean
        public UserSecurityService userSecurityService() {
            return new UserSecurityService();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private TransactionService transactionService;

    private MockMvc mockMvc;

    private MockHttpSession session;

    @Before
    public void setUp() {
        reset(transactionService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();

        session = new MockHttpSession();
        UsernamePasswordAuthenticationToken alice = new UsernamePasswordAuthenticationToken("alice", null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, new SecurityContextImpl(alice));
        session.setAttribute(TOKEN_ATTRIBUTE, new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "expected-token"));
    }

    @Test
    public void deleteWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").session(session).param("recipientId", "7"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).deleteRecipientById(anyLong(), any(Principal.class));
    }

    @Test
    public void deleteWithWrongCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").session(session).param("recipientId", "7")
                .param("_csrf", "forged-token"))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).deleteRecipientById(anyLong(), any(Principal.class));
    }

    @Test
    public void deleteWithValidCsrfTokenSucceeds() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").session(session).param("recipientId", "7")
                .param("_csrf", "expected-token"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/transfer/recipient"));

        verify(transactionService).deleteRecipientById(eq(7L), any(Principal.class));
    }
}
