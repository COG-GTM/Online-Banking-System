package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.service.AppointmentService;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AppointmentController appointmentController;

    private MockMvc mockMvc;

    private User alice;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController).build();
        alice = new User();
        alice.setUsername("alice");
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(appointmentService.createAppointment(any(Appointment.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    public void createIgnoresClientSuppliedIdConfirmedAndUser() throws Exception {
        Principal principal = () -> "alice";

        mockMvc.perform(post("/appointment/create")
                .principal(principal)
                .param("id", "42")
                .param("confirmed", "true")
                .param("user.username", "bob")
                .param("user.userId", "7")
                .param("dateString", "2026-10-20 10:30")
                .param("location", "Dhaka")
                .param("description", "Loan discussion"))
                .andExpect(view().name("redirect:/userFront"));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentService).createAppointment(captor.capture());
        Appointment saved = captor.getValue();

        assertNull(saved.getId());
        assertFalse(saved.isConfirmed());
        assertSame(alice, saved.getUser());
        assertEquals("Dhaka", saved.getLocation());
        assertEquals("Loan discussion", saved.getDescription());
    }
}
