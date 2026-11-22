package com.userfront.resource;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.userfront.service.AppointmentService;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentResourceTest {

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentResource appointmentResource;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentResource).build();
    }

    @Test
    public void confirmRejectsGet() throws Exception {
        mockMvc.perform(get("/api/appointment/7/confirm")).andExpect(status().isMethodNotAllowed());
        verify(appointmentService, never()).confirmAppointment(anyLong());
    }

    @Test
    public void confirmAcceptsPost() throws Exception {
        mockMvc.perform(post("/api/appointment/7/confirm")).andExpect(status().isOk());
        verify(appointmentService).confirmAppointment(7L);
    }
}
