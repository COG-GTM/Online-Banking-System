package com.userfront.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import javax.servlet.http.Cookie;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.controller.TransferController;
import com.userfront.dao.UserDao;
import com.userfront.resource.AppointmentResource;
import com.userfront.resource.UserResource;
import com.userfront.service.AppointmentService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = CsrfProtectionTest.TestConfig.class)
public class CsrfProtectionTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, UserResource.class, AppointmentResource.class, TransferController.class})
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
        public AppointmentService appointmentService() {
            return mock(AppointmentService.class);
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
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AppointmentService appointmentService;

    private MockMvc mockMvc;

    private MockHttpSession adminSession;

    private MockHttpSession userSession;

    @Before
    public void setUp() {
        reset(userService, transactionService, appointmentService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        adminSession = sessionFor("admin", "ROLE_ADMIN");
        userSession = sessionFor("alice", "ROLE_USER");
    }

    private static MockHttpSession sessionFor(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, null,
                Collections.singletonList(new SimpleGrantedAuthority(role)));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(auth));
        return session;
    }

    private Cookie csrfCookie(MockHttpSession session) throws Exception {
        Cookie cookie = mockMvc.perform(get("/api/appointment/all").session(session))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        return cookie;
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, MockHttpSession session)
            throws Exception {
        Cookie csrf = csrfCookie(session);
        return request.session(session).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
    }

    @Test
    public void adminUserToggleRejectsGet() throws Exception {
        mockMvc.perform(get("/api/user/bob/disable").session(adminSession)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(get("/api/user/bob/enable").session(adminSession)).andExpect(status().isMethodNotAllowed());
        verify(userService, never()).disableUser(anyString());
        verify(userService, never()).enableUser(anyString());
    }

    @Test
    public void adminUserToggleRejectsPostWithoutToken() throws Exception {
        mockMvc.perform(post("/api/user/bob/disable").session(adminSession)).andExpect(status().isForbidden());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void adminUserToggleRejectsForgedToken() throws Exception {
        mockMvc.perform(post("/api/user/bob/disable").session(adminSession)
                .cookie(csrfCookie(adminSession)).header("X-XSRF-TOKEN", "forged"))
                .andExpect(status().isForbidden());
        verify(userService, never()).disableUser(anyString());
    }

    @Test
    public void adminUserToggleAcceptsPostWithToken() throws Exception {
        mockMvc.perform(withToken(post("/api/user/bob/disable"), adminSession)).andExpect(status().isOk());
        mockMvc.perform(withToken(post("/api/user/bob/enable"), adminSession)).andExpect(status().isOk());
        verify(userService).disableUser("bob");
        verify(userService).enableUser("bob");
    }

    @Test
    public void appointmentConfirmRequiresPostWithToken() throws Exception {
        mockMvc.perform(get("/api/appointment/7/confirm").session(adminSession)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/appointment/7/confirm").session(adminSession)).andExpect(status().isForbidden());
        verify(appointmentService, never()).confirmAppointment(anyLong());

        mockMvc.perform(withToken(post("/api/appointment/7/confirm"), adminSession)).andExpect(status().isOk());
        verify(appointmentService).confirmAppointment(7L);
    }

    @Test
    public void recipientDeleteRejectsGet() throws Exception {
        mockMvc.perform(get("/transfer/recipient/delete").param("recipientName", "Mallory").session(userSession))
                .andExpect(status().isMethodNotAllowed());
        verify(transactionService, never()).deleteRecipientByName(anyString());
    }

    @Test
    public void recipientDeleteRejectsPostWithoutToken() throws Exception {
        mockMvc.perform(post("/transfer/recipient/delete").param("recipientName", "Mallory").session(userSession))
                .andExpect(status().isForbidden());
        verify(transactionService, never()).deleteRecipientByName(anyString());
    }

    @Test
    public void recipientDeleteAcceptsFormTokenParameter() throws Exception {
        Cookie csrf = csrfCookie(userSession);
        mockMvc.perform(post("/transfer/recipient/delete").param("recipientName", "Mallory")
                .param("_csrf", csrf.getValue()).cookie(csrf).session(userSession));
        verify(transactionService).deleteRecipientByName("Mallory");
    }

    @Test
    public void moneyTransferRejectsPostWithoutToken() throws Exception {
        mockMvc.perform(post("/transfer/toSomeoneElse").param("recipientName", "Mallory")
                .param("accountType", "Primary").param("amount", "500").session(userSession))
                .andExpect(status().isForbidden());
        verify(transactionService, never()).toSomeoneElseTransfer(any(), anyString(), anyString(), any(), any());
    }

    @Test
    public void logoutRequiresPostWithToken() throws Exception {
        mockMvc.perform(get("/logout").session(userSession))
                .andExpect(result -> assertNull(result.getResponse().getRedirectedUrl()));
        mockMvc.perform(post("/logout").session(userSession)).andExpect(status().isForbidden());
        mockMvc.perform(withToken(post("/logout"), userSession)).andExpect(redirectedUrl("/index?logout"));
    }
}
