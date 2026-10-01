package com.userfront.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import com.userfront.domain.Appointment;
import com.userfront.service.AppointmentService;

@RunWith(MockitoJUnitRunner.class)
public class AppointmentResourceTest {

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentResource appointmentResource;

    private Pageable requestedPage(int page, int size) {
        when(appointmentService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.<Appointment>emptyList()));
        appointmentResource.findAppointmentList(page, size);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(appointmentService).findAll(captor.capture());
        return captor.getValue();
    }

    @Test
    public void requestsNewestAppointmentsFirst() {
        Pageable pageable = requestedPage(2, 20);

        assertEquals(2, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("date").getDirection());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    public void capsPageSize() {
        assertEquals(AppointmentResource.MAX_PAGE_SIZE, requestedPage(0, 1000000).getPageSize());
    }

    @Test
    public void clampsNonPositivePageAndSize() {
        Pageable pageable = requestedPage(-3, 0);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(1, pageable.getPageSize());
    }

    @Test
    public void returnsPageContentWithTotalCountHeader() {
        Appointment appointment = new Appointment();
        List<Appointment> content = Collections.singletonList(appointment);
        when(appointmentService.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(content, invocation.getArgument(0), 137));

        ResponseEntity<List<Appointment>> response = appointmentResource.findAppointmentList(0, AppointmentResource.DEFAULT_PAGE_SIZE);

        assertEquals(content, response.getBody());
        assertEquals("137", response.getHeaders().getFirst(AppointmentResource.TOTAL_COUNT_HEADER));
    }
}
