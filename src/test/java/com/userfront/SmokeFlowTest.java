package com.userfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

import com.userfront.dao.AppointmentDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.support.AbstractIntegrationTest;

/**
 * The manual smoke flow (signup, login, money movement, recipients, appointments, profile, logout) as it behaves today.
 * Every test signs up its own user, so the tests do not depend on each other or on their order.
 */
public class SmokeFlowTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Secret123!";

    @Autowired
    private UserDao userDao;

    @Autowired
    private PrimaryTransactionDao primaryTransactionDao;

    @Autowired
    private SavingsTransactionDao savingsTransactionDao;

    @Autowired
    private RecipientDao recipientDao;

    @Autowired
    private AppointmentDao appointmentDao;

    @Test
    public void signupCreatesUserWithEmptyPrimaryAndSavingsAccounts() throws Exception {
        String username = signUp();

        User user = userDao.findByUsername(username);
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(username + "@example.com");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getPassword()).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
        assertThat(user.getUserRoles()).extracting(userRole -> userRole.getRole().getName()).containsExactly("ROLE_USER");
        assertThat(user.getPrimaryAccount().getAccountBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getSavingsAccount().getAccountBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getPrimaryAccount().getAccountNumber())
                .isNotEqualTo(user.getSavingsAccount().getAccountNumber());
    }

    @Test
    public void signupWithTakenUsernameRedisplaysFormWithError() throws Exception {
        String username = signUp();

        mockMvc.perform(post("/signup")
                .param("firstName", "Other")
                .param("lastName", "Person")
                .param("phone", "555-000-0000")
                .param("email", "other-" + username + "@example.com")
                .param("username", username)
                .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("usernameExists", true))
                .andExpect(model().attributeDoesNotExist("emailExists"));
    }

    @Test
    public void loginWithValidCredentialsLandsOnUserFront() throws Exception {
        String username = signUp();

        MvcResult result = mockMvc.perform(formLogin("/index").user(username).password(PASSWORD))
                .andExpect(redirectedUrl("/userFront"))
                .andExpect(authenticated().withUsername(username))
                .andReturn();

        SecurityContext context = (SecurityContext) result.getRequest().getSession(false)
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    public void loginWithWrongPasswordRedirectsToIndexWithError() throws Exception {
        String username = signUp();

        mockMvc.perform(formLogin("/index").user(username).password("wrong-password"))
                .andExpect(redirectedUrl("/index?error"))
                .andExpect(unauthenticated());
    }

    @Test
    public void loginWithRememberMeSetsRememberMeCookie() throws Exception {
        String username = signUp();

        mockMvc.perform(post("/index")
                .param("username", username)
                .param("password", PASSWORD)
                .param("remember-me", "on"))
                .andExpect(redirectedUrl("/userFront"))
                .andExpect(cookie().exists("remember-me"));
    }

    @Test
    public void userFrontShowsBothAccounts() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        mockMvc.perform(get("/userFront").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("userFront"))
                .andExpect(model().attributeExists("primaryAccount", "savingsAccount"));
    }

    @Test
    public void depositAddsToBalanceAndRecordsTransaction() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        deposit(session, "Primary", "100");
        deposit(session, "Savings", "40");

        User user = userDao.findByUsername(username);
        assertThat(user.getPrimaryAccount().getAccountBalance()).isEqualByComparingTo("100");
        assertThat(user.getSavingsAccount().getAccountBalance()).isEqualByComparingTo("40");
        assertThat(primaryTransactions(user)).extracting(PrimaryTransaction::getDescription)
                .containsExactly("Deposit to Primary Account");
        assertThat(savingsTransactions(user)).extracting(SavingsTransaction::getDescription)
                .containsExactly("Deposit to savings Account");
    }

    @Test
    public void withdrawSubtractsFromBalanceAndRecordsTransaction() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        deposit(session, "Primary", "100");

        mockMvc.perform(post("/account/withdraw").session(session)
                .param("accountType", "Primary")
                .param("amount", "30"))
                .andExpect(redirectedUrl("/userFront"));

        User user = userDao.findByUsername(username);
        assertThat(user.getPrimaryAccount().getAccountBalance()).isEqualByComparingTo("70");
        assertThat(primaryTransactions(user)).extracting(PrimaryTransaction::getDescription)
                .containsExactlyInAnyOrder("Deposit to Primary Account", "Withdraw from Primary Account");
    }

    @Test
    public void transferBetweenAccountsMovesMoneyBothWays() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        deposit(session, "Primary", "100");

        transferBetweenAccounts(session, "Primary", "Savings", "20");
        transferBetweenAccounts(session, "Savings", "Primary", "5");

        User user = userDao.findByUsername(username);
        assertThat(user.getPrimaryAccount().getAccountBalance()).isEqualByComparingTo("85");
        assertThat(user.getSavingsAccount().getAccountBalance()).isEqualByComparingTo("15");
        assertThat(primaryTransactions(user)).extracting(PrimaryTransaction::getDescription)
                .contains("Between account transfer from Primary to Savings");
        assertThat(savingsTransactions(user)).extracting(SavingsTransaction::getDescription)
                .containsExactly("Between account transfer from Savings to Primary");
    }

    @Test
    public void addedRecipientIsListed() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        String recipientName = "Recipient " + username;

        addRecipient(session, recipientName);

        mockMvc.perform(get("/transfer/recipient").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("recipient"))
                .andExpect(content().string(containsString(recipientName)));
        Recipient recipient = recipientDao.findByName(recipientName);
        assertThat(recipient.getUser().getUsername()).isEqualTo(username);
        assertThat(recipient.getAccountNumber()).isEqualTo("987654321");
    }

    @Test
    public void transferToRecipientDebitsChosenAccount() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        deposit(session, "Primary", "100");
        String recipientName = "Recipient " + username;
        addRecipient(session, recipientName);

        mockMvc.perform(post("/transfer/toSomeoneElse").session(session)
                .param("recipientName", recipientName)
                .param("accountType", "Primary")
                .param("amount", "5"))
                .andExpect(redirectedUrl("/userFront"));

        User user = userDao.findByUsername(username);
        assertThat(user.getPrimaryAccount().getAccountBalance()).isEqualByComparingTo("95");
        assertThat(primaryTransactions(user)).extracting(PrimaryTransaction::getDescription)
                .contains("Transfer to recipient " + recipientName);
    }

    @Test
    public void createdAppointmentIsStoredUnconfirmed() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        mockMvc.perform(post("/appointment/create").session(session)
                .param("dateString", "2030-01-15 09:30")
                .param("location", "Chicago")
                .param("description", "Open a new account"))
                .andExpect(redirectedUrl("/userFront"));

        List<Appointment> appointments = appointmentDao.findAll().stream()
                .filter(appointment -> username.equals(appointment.getUser().getUsername()))
                .collect(Collectors.toList());
        assertThat(appointments).hasSize(1);
        Appointment appointment = appointments.get(0);
        assertThat(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(appointment.getDate())).isEqualTo("2030-01-15 09:30");
        assertThat(appointment.getLocation()).isEqualTo("Chicago");
        assertThat(appointment.getDescription()).isEqualTo("Open a new account");
        assertThat(appointment.isConfirmed()).isFalse();
    }

    @Test
    public void accountPagesListTransactions() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        deposit(session, "Primary", "100");
        deposit(session, "Savings", "40");

        mockMvc.perform(get("/account/primaryAccount").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("primaryAccount"))
                .andExpect(content().string(containsString("Deposit to Primary Account")));
        mockMvc.perform(get("/account/savingsAccount").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("savingsAccount"))
                .andExpect(content().string(containsString("Deposit to savings Account")));
    }

    @Test
    public void profileUpdateSavesChangedFields() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        mockMvc.perform(get("/user/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(content().string(containsString(username + "@example.com")));

        mockMvc.perform(post("/user/profile").session(session)
                .param("username", username)
                .param("firstName", "Janet")
                .param("lastName", "Smith")
                .param("email", "new-" + username + "@example.com")
                .param("phone", "555-999-0000"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));

        User user = userDao.findByUsername(username);
        assertThat(user.getFirstName()).isEqualTo("Janet");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getEmail()).isEqualTo("new-" + username + "@example.com");
        assertThat(user.getPhone()).isEqualTo("555-999-0000");
    }

    @Test
    public void adminApiIsForbiddenForSignedUpUser() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        mockMvc.perform(get("/api/user/all").session(session)).andExpect(status().isForbidden());
    }

    @Test
    public void logoutEndsSessionAndProtectedPagesRedirectToLogin() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        mockMvc.perform(get("/logout").session(session))
                .andExpect(redirectedUrl("/index?logout"))
                .andExpect(unauthenticated());

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/userFront"))
                .andExpect(redirectedUrl("http://localhost/index"));
    }

    private String signUp() throws Exception {
        String username = "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(post("/signup")
                .param("firstName", "Jane")
                .param("lastName", "Doe")
                .param("phone", "555-123-4567")
                .param("email", username + "@example.com")
                .param("username", username)
                .param("password", PASSWORD))
                .andExpect(redirectedUrl("/"));
        return username;
    }

    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin("/index").user(username).password(PASSWORD))
                .andExpect(redirectedUrl("/userFront"))
                .andReturn().getRequest().getSession(false);
    }

    private void deposit(MockHttpSession session, String accountType, String amount) throws Exception {
        mockMvc.perform(post("/account/deposit").session(session)
                .param("accountType", accountType)
                .param("amount", amount))
                .andExpect(redirectedUrl("/userFront"));
    }

    private void transferBetweenAccounts(MockHttpSession session, String from, String to, String amount) throws Exception {
        mockMvc.perform(post("/transfer/betweenAccounts").session(session)
                .param("transferFrom", from)
                .param("transferTo", to)
                .param("amount", amount))
                .andExpect(redirectedUrl("/userFront"));
    }

    private void addRecipient(MockHttpSession session, String name) throws Exception {
        mockMvc.perform(post("/transfer/recipient/save").session(session)
                .param("name", name)
                .param("email", "recipient@example.com")
                .param("phone", "555-222-3333")
                .param("accountNumber", "987654321")
                .param("description", "Rent"))
                .andExpect(redirectedUrl("/transfer/recipient"));
    }

    private List<PrimaryTransaction> primaryTransactions(User user) {
        Long accountId = user.getPrimaryAccount().getId();
        return primaryTransactionDao.findAll().stream()
                .filter(transaction -> accountId.equals(transaction.getPrimaryAccount().getId()))
                .collect(Collectors.toList());
    }

    private List<SavingsTransaction> savingsTransactions(User user) {
        Long accountId = user.getSavingsAccount().getId();
        return savingsTransactionDao.findAll().stream()
                .filter(transaction -> accountId.equals(transaction.getSavingsAccount().getId()))
                .collect(Collectors.toList());
    }
}
