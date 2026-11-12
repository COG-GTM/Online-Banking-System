package com.userfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;

import com.userfront.dao.AppointmentDao;
import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.AppointmentService;
import com.userfront.support.AbstractIntegrationTest;

/**
 * Data rules that must hold on a database that already has data, e.g. after an app restart.
 */
public class DataIntegrityTest extends AbstractIntegrationTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private AppointmentDao appointmentDao;

    @Autowired
    private AppointmentService appointmentService;

    /** Accounts left by an earlier run hold the numbers an in-memory counter would hand out next. */
    @Test
    public void signupOnDatabaseWithExistingAccountsGetsUnusedAccountNumbers() throws Exception {
        int highest = Math.max(11223145, Math.max(maxOrZero(primaryAccountDao.findMaxAccountNumber()),
                maxOrZero(savingsAccountDao.findMaxAccountNumber())));
        PrimaryAccount existingPrimary = new PrimaryAccount();
        existingPrimary.setAccountNumber(highest + 1);
        existingPrimary.setAccountBalance(BigDecimal.ZERO);
        primaryAccountDao.save(existingPrimary);
        SavingsAccount existingSavings = new SavingsAccount();
        existingSavings.setAccountNumber(highest + 2);
        existingSavings.setAccountBalance(BigDecimal.ZERO);
        savingsAccountDao.save(existingSavings);

        String username = signUp(uniqueName());

        User user = userDao.findByUsername(username);
        assertThat(user.getPrimaryAccount().getAccountNumber()).isGreaterThan(highest + 2);
        assertThat(user.getSavingsAccount().getAccountNumber()).isGreaterThan(highest + 2)
                .isNotEqualTo(user.getPrimaryAccount().getAccountNumber());
    }

    @Test
    public void databaseRejectsDuplicateAccountNumber() {
        int taken = maxOrZero(primaryAccountDao.findMaxAccountNumber()) + 1000;
        PrimaryAccount first = new PrimaryAccount();
        first.setAccountNumber(taken);
        first.setAccountBalance(BigDecimal.ZERO);
        primaryAccountDao.save(first);

        PrimaryAccount duplicate = new PrimaryAccount();
        duplicate.setAccountNumber(taken);
        duplicate.setAccountBalance(BigDecimal.ZERO);
        assertThatThrownBy(() -> primaryAccountDao.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void databaseRejectsDuplicateUsername() throws Exception {
        String username = signUp(uniqueName());

        User duplicate = new User();
        duplicate.setUsername(username);
        duplicate.setEmail("other-" + username + "@example.com");
        assertThatThrownBy(() -> userDao.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void databaseRejectsDuplicateEmail() throws Exception {
        String username = signUp(uniqueName());

        User duplicate = new User();
        duplicate.setUsername(uniqueName());
        duplicate.setEmail(username + "@example.com");
        assertThatThrownBy(() -> userDao.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void signupWithTakenEmailRedisplaysFormWithError() throws Exception {
        String existing = signUp(uniqueName());
        String newUsername = uniqueName();

        mockMvc.perform(post("/signup")
                .param("firstName", "Other")
                .param("lastName", "Person")
                .param("phone", "555-000-0000")
                .param("email", existing + "@example.com")
                .param("username", newUsername)
                .param("password", "Secret123!"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"))
                .andExpect(model().attribute("emailExists", true))
                .andExpect(model().attributeDoesNotExist("usernameExists"));
        assertThat(userDao.findByUsername(newUsername)).isNull();
    }

    @Test
    public void findAppointmentReturnsStoredAppointment() throws Exception {
        Appointment appointment = saveAppointment();

        Appointment found = appointmentService.findAppointment(appointment.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(appointment.getId());
        assertThat(found.getLocation()).isEqualTo("Chicago");
        assertThat(appointmentService.findAppointment(-1L)).isNull();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void adminCanConfirmAppointment() throws Exception {
        Appointment appointment = saveAppointment();

        mockMvc.perform(get("/api/appointment/" + appointment.getId() + "/confirm")).andExpect(status().isOk());

        assertThat(appointmentDao.findById(appointment.getId()).get().isConfirmed()).isTrue();
    }

    private Appointment saveAppointment() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setUser(userDao.findByUsername(signUp(uniqueName())));
        appointment.setDate(new Date());
        appointment.setLocation("Chicago");
        appointment.setDescription("Open a new account");
        appointment.setConfirmed(false);
        return appointmentDao.save(appointment);
    }

    private String signUp(String username) throws Exception {
        mockMvc.perform(post("/signup")
                .param("firstName", "Test")
                .param("lastName", "User")
                .param("phone", "555-123-4567")
                .param("email", username + "@example.com")
                .param("username", username)
                .param("password", "Secret123!"))
                .andExpect(redirectedUrl("/"));
        return username;
    }

    private static String uniqueName() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static int maxOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
