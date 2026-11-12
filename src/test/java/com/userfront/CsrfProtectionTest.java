package com.userfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.userfront.dao.AppointmentDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.support.AbstractIntegrationTest;

/**
 * State-changing requests need a CSRF token and are POST-only, so a forged link or cross-site form cannot trigger them.
 */
public class CsrfProtectionTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "Secret123!";
    private static final String CSRF_FIELD = "<input type=\"hidden\" name=\"_csrf\"";

    @Autowired
    private UserDao userDao;

    @Autowired
    private AppointmentDao appointmentDao;

    @Autowired
    private RecipientDao recipientDao;

    @Test
    public void loginAndSignupFormsCarryTheToken() throws Exception {
        mockMvc.perform(get("/index")).andExpect(content().string(containsString(CSRF_FIELD)));
        mockMvc.perform(get("/signup")).andExpect(content().string(containsString(CSRF_FIELD)));
    }

    @Test
    public void userFrontFormsCarryTheToken() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        addRecipient(session, "Recipient " + username);

        for (String page : new String[] {"/account/deposit", "/account/withdraw", "/transfer/betweenAccounts",
                "/transfer/toSomeoneElse", "/transfer/recipient", "/appointment/create", "/user/profile"}) {
            mockMvc.perform(get(page).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(CSRF_FIELD)))
                    .andExpect(content().string(not(containsString("${_csrf"))));
        }
    }

    @Test
    public void signupWithoutTokenIsRejected() throws Exception {
        String username = "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(signupRequest(username)).andExpect(status().isForbidden());
        assertThat(userDao.findByUsername(username)).isNull();
    }

    @Test
    public void depositWithoutTokenIsRejected() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);

        mockMvc.perform(post("/account/deposit").session(session)
                .param("accountType", "Primary")
                .param("amount", "100"))
                .andExpect(status().isForbidden());

        assertThat(userDao.findByUsername(username).getPrimaryAccount().getAccountBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    public void recipientDeleteIsPostOnlyAndNeedsToken() throws Exception {
        String username = signUp();
        MockHttpSession session = login(username);
        String recipientName = "Recipient " + username;
        addRecipient(session, recipientName);

        mockMvc.perform(get("/transfer/recipient/delete").session(session).param("recipientName", recipientName))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/transfer/recipient/delete").session(session).param("recipientName", recipientName))
                .andExpect(status().isForbidden());
        assertThat(recipientDao.findByName(recipientName)).isNotNull();

        mockMvc.perform(post("/transfer/recipient/delete").with(csrf()).session(session).param("recipientName", recipientName))
                .andExpect(status().isOk());
        assertThat(recipientDao.findByName(recipientName)).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void appointmentConfirmIsPostOnlyAndNeedsToken() throws Exception {
        Appointment appointment = saveAppointment();
        String url = "/api/appointment/" + appointment.getId() + "/confirm";

        mockMvc.perform(get(url)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post(url)).andExpect(status().isForbidden());
        assertThat(appointmentDao.findById(appointment.getId()).get().isConfirmed()).isFalse();

        mockMvc.perform(post(url).with(csrf())).andExpect(status().isOk());
        assertThat(appointmentDao.findById(appointment.getId()).get().isConfirmed()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void userEnableAndDisableArePostOnlyAndNeedToken() throws Exception {
        String username = signUp();

        mockMvc.perform(get("/api/user/" + username + "/disable")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/user/" + username + "/disable")).andExpect(status().isForbidden());
        assertThat(userDao.findByUsername(username).isEnabled()).isTrue();

        mockMvc.perform(post("/api/user/" + username + "/disable").with(csrf())).andExpect(status().isOk());
        assertThat(userDao.findByUsername(username).isEnabled()).isFalse();

        mockMvc.perform(get("/api/user/" + username + "/enable")).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(post("/api/user/" + username + "/enable")).andExpect(status().isForbidden());
        assertThat(userDao.findByUsername(username).isEnabled()).isFalse();

        mockMvc.perform(post("/api/user/" + username + "/enable").with(csrf())).andExpect(status().isOk());
        assertThat(userDao.findByUsername(username).isEnabled()).isTrue();
    }

    private MockHttpServletRequestBuilder signupRequest(String username) {
        return post("/signup")
                .param("firstName", "Jane")
                .param("lastName", "Doe")
                .param("phone", "555-123-4567")
                .param("email", username + "@example.com")
                .param("username", username)
                .param("password", PASSWORD);
    }

    private String signUp() throws Exception {
        String username = "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mockMvc.perform(signupRequest(username).with(csrf())).andExpect(redirectedUrl("/"));
        return username;
    }

    private MockHttpSession login(String username) throws Exception {
        return (MockHttpSession) mockMvc.perform(formLogin("/index").user(username).password(PASSWORD))
                .andExpect(redirectedUrl("/userFront"))
                .andReturn().getRequest().getSession(false);
    }

    private void addRecipient(MockHttpSession session, String name) throws Exception {
        mockMvc.perform(post("/transfer/recipient/save").with(csrf()).session(session)
                .param("name", name)
                .param("email", "recipient@example.com")
                .param("phone", "555-222-3333")
                .param("accountNumber", "987654321")
                .param("description", "Rent"))
                .andExpect(redirectedUrl("/transfer/recipient"));
    }

    private Appointment saveAppointment() throws Exception {
        User user = userDao.findByUsername(signUp());
        Appointment appointment = new Appointment();
        appointment.setDate(new Date());
        appointment.setLocation("Chicago");
        appointment.setDescription("CSRF check");
        appointment.setUser(user);
        return appointmentDao.save(appointment);
    }
}
