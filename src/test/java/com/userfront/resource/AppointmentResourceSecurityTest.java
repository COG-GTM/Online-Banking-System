package com.userfront.resource;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.config.SecurityConfig;
import com.userfront.dao.UserDao;
import com.userfront.service.AppointmentNotFoundException;
import com.userfront.service.AppointmentService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AppointmentResourceSecurityTest.Config.class)
public class AppointmentResourceSecurityTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, AppointmentResource.class})
    static class Config {

        @Bean
        AppointmentService appointmentService() {
            return Mockito.mock(AppointmentService.class);
        }

        @Bean
        UserDao userDao() {
            return Mockito.mock(UserDao.class);
        }

        @Bean
        UserSecurityService userSecurityService() {
            return new UserSecurityService();
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppointmentService appointmentService;

    private MockMvc mvc;

    @Before
    public void setUp() {
        reset(appointmentService);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void adminCanConfirmWithPost() throws Exception {
        mvc.perform(post("/api/appointment/5/confirm").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(appointmentService).confirmAppointment(5L);
    }

    @Test
    public void confirmRejectsGet() throws Exception {
        mvc.perform(get("/api/appointment/5/confirm").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());

        verify(appointmentService, never()).confirmAppointment(anyLong());
    }

    @Test
    public void confirmRejectsPut() throws Exception {
        mvc.perform(put("/api/appointment/5/confirm").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());

        verify(appointmentService, never()).confirmAppointment(anyLong());
    }

    @Test
    public void nonAdminCannotConfirm() throws Exception {
        mvc.perform(post("/api/appointment/5/confirm").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).confirmAppointment(anyLong());
    }

    @Test
    public void anonymousCannotConfirm() throws Exception {
        mvc.perform(post("/api/appointment/5/confirm"))
                .andExpect(status().is3xxRedirection());

        verify(appointmentService, never()).confirmAppointment(anyLong());
    }

    @Test
    public void confirmingUnknownAppointmentReturnsNotFound() throws Exception {
        doThrow(new AppointmentNotFoundException(99L)).when(appointmentService).confirmAppointment(99L);

        mvc.perform(post("/api/appointment/99/confirm").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void appointmentListStaysAdminOnlyGet() throws Exception {
        mvc.perform(get("/api/appointment/all").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/appointment/all").with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/appointment/all").with(user("admin").roles("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
    }
}
