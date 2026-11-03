package com.userfront.resource;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.config.SecurityConfig;
import com.userfront.dao.UserDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = UserResourceAuthorizationTest.TestConfig.class)
public class UserResourceAuthorizationTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, UserResource.class})
    static class TestConfig {

        @Bean
        public UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        public TransactionService transactionService() {
            return Mockito.mock(TransactionService.class);
        }

        @Bean
        public UserSecurityService userSecurityService() {
            return Mockito.mock(UserSecurityService.class);
        }

        @Bean
        public UserDao userDao() {
            return Mockito.mock(UserDao.class);
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserService userService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        reset(userService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void anonymousRequestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/api/user/all"))
                .andExpect(status().is3xxRedirection());
        verify(userService, never()).findUserList();
    }

    @Test
    public void nonAdminUserIsForbidden() throws Exception {
        mockMvc.perform(get("/api/user/all").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        verify(userService, never()).findUserList();
    }

    @Test
    public void nonAdminUserIsForbiddenOnOtherAdminApiRoutes() throws Exception {
        mockMvc.perform(get("/api/user/primary/transaction").param("username", "bob")
                .with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void nonAdminUserIsForbiddenOnBareApiPath() throws Exception {
        mockMvc.perform(get("/api").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void adminReceivesSummariesWithoutCredentialFields() throws Exception {
        when(userService.findUserList()).thenReturn(Collections.singletonList(sampleUser()));

        mockMvc.perform(get("/api/user/all").with(user("root").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(7))
                .andExpect(jsonPath("$[0].username").value("bob"))
                .andExpect(jsonPath("$[0].firstName").value("Bob"))
                .andExpect(jsonPath("$[0].email").value("bob@example.com"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].primaryAccount.accountNumber").value(11223101))
                .andExpect(jsonPath("$[0].primaryAccount.accountBalance").value(150.25))
                .andExpect(jsonPath("$[0].savingsAccount.accountBalance").value(900.0))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].recipientList").doesNotExist())
                .andExpect(jsonPath("$[0].authorities").doesNotExist())
                .andExpect(content().string(not(containsString("$2a$12$"))))
                .andExpect(content().string(not(containsString("99887766"))));
    }

    @Test
    public void userToStringOmitsPasswordHash() {
        String rendered = sampleUser().toString();
        org.junit.Assert.assertFalse(rendered.contains("$2a$12$"));
        org.junit.Assert.assertFalse(rendered.contains("99887766"));
    }

    private static User sampleUser() {
        PrimaryAccount primary = new PrimaryAccount();
        primary.setId(1L);
        primary.setAccountNumber(11223101);
        primary.setAccountBalance(new BigDecimal("150.25"));

        SavingsAccount savings = new SavingsAccount();
        savings.setId(2L);
        savings.setAccountNumber(11223102);
        savings.setAccountBalance(new BigDecimal("900.00"));

        Recipient payee = new Recipient();
        payee.setName("Landlord");
        payee.setAccountNumber("99887766");

        User user = new User();
        user.setUserId(7L);
        user.setUsername("bob");
        user.setPassword("$2a$12$abcdefghijklmnopqrstuvC2xZ8u7l3m9y0w1q2r3s4t5u6v7w8x9y");
        user.setFirstName("Bob");
        user.setLastName("Builder");
        user.setEmail("bob@example.com");
        user.setPhone("555-0100");
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);
        user.setRecipientList(new ArrayList<>(Collections.singletonList(payee)));
        payee.setUser(user);
        return user;
    }
}
