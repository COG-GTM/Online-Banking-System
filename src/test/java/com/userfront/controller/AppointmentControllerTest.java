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

import com.userfront.dao.AppointmentDao;
import com.userfront.domain.Appointment;
import com.userfront.domain.User;
import com.userfront.service.AppointmentService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.AppointmentServiceImpl;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private UserService userService;

    @Mock
    private AppointmentDao appointmentDao;

    @InjectMocks
    private AppointmentController appointmentController;

    @InjectMocks
    private AppointmentServiceImpl appointmentServiceImpl;

    private MockMvc mockMvc;

    private User customer;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController).build();
        customer = new User();
        customer.setUserId(1L);
        customer.setUsername("customer");
    }

    @Test
    public void createAppointmentPostIgnoresClientControlledFields() throws Exception {
        when(userService.findByUsername("customer")).thenReturn(customer);
        Principal principal = () -> "customer";

        mockMvc.perform(post("/appointment/create")
                .principal(principal)
                .param("dateString", "2026-11-10 10:30")
                .param("location", "Dhaka")
                .param("description", "Loan question")
                .param("id", "42")
                .param("confirmed", "true")
                .param("user.userId", "2")
                .param("user.username", "victim"))
                .andExpect(view().name("redirect:/userFront"));

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentService).createAppointment(captor.capture());
        Appointment saved = captor.getValue();

        assertNull(saved.getId());
        assertFalse(saved.isConfirmed());
        assertSame(customer, saved.getUser());
        assertEquals("Dhaka", saved.getLocation());
        assertEquals("Loan question", saved.getDescription());
    }

    @Test
    public void createAppointmentAlwaysPersistsNewUnconfirmedAppointment() {
        when(appointmentDao.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Appointment appointment = new Appointment();
        appointment.setId(42L);
        appointment.setConfirmed(true);

        Appointment saved = appointmentServiceImpl.createAppointment(appointment);

        assertNull(saved.getId());
        assertFalse(saved.isConfirmed());
    }
}
