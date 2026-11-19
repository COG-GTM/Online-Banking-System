package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.service.AppointmentService;
import com.userfront.service.UserService;

public class AppointmentControllerTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Principal ALICE = () -> "alice";

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        AppointmentController controller = new AppointmentController();
        ReflectionTestUtils.setField(controller, "appointmentService", appointmentService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "appointmentDateParser", new AppointmentDateParser(
                Clock.fixed(LocalDateTime.of(2026, 6, 15, 9, 0).atZone(ZONE).toInstant(), ZONE)));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(userService.findByUsername("alice")).thenReturn(new User());
    }

    @Test
    public void validFutureDateIsSaved() throws Exception {
        mockMvc.perform(post("/appointment/create").principal(ALICE)
                .param("dateString", "2026-06-20 12:30")
                .param("location", "Dhaka")
                .param("description", "Loan"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/userFront"));

        ArgumentCaptor<Appointment> saved = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentService).createAppointment(saved.capture());
        assertEquals(Date.from(LocalDateTime.of(2026, 6, 20, 12, 30).atZone(ZONE).toInstant()), saved.getValue().getDate());
        assertEquals("Dhaka", saved.getValue().getLocation());
    }

    @Test
    public void lenientlyRolledDateIsRejected() throws Exception {
        assertFormRedisplayed("2026-13-45 99:99");
    }

    @Test
    public void trailingGarbageIsRejected() throws Exception {
        assertFormRedisplayed("2026-06-20 10:00<script>");
    }

    @Test
    public void pastDateIsRejected() throws Exception {
        assertFormRedisplayed("2020-01-01 10:00");
    }

    @Test
    public void missingDateIsRejectedWithoutServerError() throws Exception {
        mockMvc.perform(post("/appointment/create").principal(ALICE).param("location", "Dhaka"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment"))
                .andExpect(model().attributeExists("dateError"));
        verify(appointmentService, never()).createAppointment(any());
    }

    private void assertFormRedisplayed(String dateString) throws Exception {
        mockMvc.perform(post("/appointment/create").principal(ALICE)
                .param("dateString", dateString)
                .param("location", "Dhaka"))
                .andExpect(status().isOk())
                .andExpect(view().name("appointment"))
                .andExpect(model().attribute("dateString", dateString))
                .andExpect(model().attributeExists("dateError"));
        verify(appointmentService, never()).createAppointment(any());
    }
}
