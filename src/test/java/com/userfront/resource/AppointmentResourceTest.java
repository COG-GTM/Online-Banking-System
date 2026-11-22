package com.userfront.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userfront.domain.Appointment;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.User;
import com.userfront.resource.dto.AppointmentSummary;
import com.userfront.service.AppointmentService;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppointmentResourceTest.Config.class)
public class AppointmentResourceTest {

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    static class Config {
        @Bean
        public AppointmentService appointmentService() {
            return mock(AppointmentService.class);
        }

        @Bean
        public AppointmentResource appointmentResource() {
            return new AppointmentResource();
        }
    }

    @Autowired
    private AppointmentResource appointmentResource;

    @Autowired
    private AppointmentService appointmentService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @After
    public void tearDown() {
        SecurityContextHolder.clearContext();
        reset(appointmentService);
    }

    @Test(expected = AccessDeniedException.class)
    public void regularUserCannotListAppointments() {
        authenticate("ROLE_USER");
        try {
            appointmentResource.findAppointmentList();
        } finally {
            verify(appointmentService, never()).findAllSummaries();
            verify(appointmentService, never()).findAll();
        }
    }

    @Test
    public void adminListIsProjectionWithoutNestedUser() throws Exception {
        authenticate("ROLE_ADMIN");
        when(appointmentService.findAllSummaries()).thenReturn(Collections.singletonList(
                new AppointmentSummary(7L, new Date(0), "Chicago", "loan", false, "alice")));

        List<AppointmentSummary> result = appointmentResource.findAppointmentList();
        String json = objectMapper.writeValueAsString(result);

        assertEquals(1, result.size());
        assertTrue(json.contains("\"username\":\"alice\""));
        assertFalse(json.contains("\"user\""));
        assertFalse(json.contains("password"));
        verify(appointmentService, never()).findAll();
    }

    @Test
    public void appointmentEntityDoesNotSerializeOrPrintUser() throws Exception {
        User user = new User();
        user.setUsername("alice");
        user.setPassword("$2a$12$secrethash");
        user.setEmail("alice@example.com");
        PrimaryAccount primary = new PrimaryAccount();
        primary.setAccountNumber(11223101);
        user.setPrimaryAccount(primary);

        Appointment appointment = new Appointment();
        appointment.setId(7L);
        appointment.setUser(user);

        String json = objectMapper.writeValueAsString(appointment);
        assertFalse(json.contains("\"user\""));
        assertFalse(json.contains("secrethash"));
        assertFalse(json.contains("alice@example.com"));
        assertFalse(json.contains("11223101"));

        String text = appointment.toString();
        assertTrue(text.contains("username=alice"));
        assertFalse(text.contains("secrethash"));
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "caller", "n/a", AuthorityUtils.createAuthorityList(role)));
    }
}
