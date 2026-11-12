package com.userfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import com.userfront.dao.AppointmentDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.support.AbstractIntegrationTest;

/**
 * The token flow a JavaScript admin client relies on: read the XSRF-TOKEN cookie, echo it as X-XSRF-TOKEN.
 * <p>
 * Spring Security's test {@code csrf()} post-processor permanently swaps the shared filter's token repository
 * for a session-based one, so this class starts from a fresh context and must never use {@code csrf()}.
 */
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class CsrfCookieTokenTest extends AbstractIntegrationTest {

    @Autowired
    private AppointmentDao appointmentDao;

    @Autowired
    private UserDao userDao;

    @Test
    public void tokenCookieIsReadableByJavaScript() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    public void adminCanConfirmWithTheTokenFromTheCookie() throws Exception {
        User owner = new User();
        owner.setUsername("cookie" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        owner.setEmail(owner.getUsername() + "@example.com");
        Appointment appointment = new Appointment();
        appointment.setUser(userDao.save(owner));
        appointment.setDate(new Date());
        appointment.setLocation("Chicago");
        appointment.setDescription("CSRF cookie check");
        appointment = appointmentDao.save(appointment);
        Cookie token = mockMvc.perform(get("/index")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();

        mockMvc.perform(post("/api/appointment/" + appointment.getId() + "/confirm")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        assertThat(appointmentDao.findById(appointment.getId()).get().isConfirmed()).isTrue();
    }

    @Test
    public void cookieWithoutMatchingHeaderIsRejected() throws Exception {
        Cookie token = mockMvc.perform(get("/index")).andReturn().getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/appointment/-1/confirm")
                .cookie(token)
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
