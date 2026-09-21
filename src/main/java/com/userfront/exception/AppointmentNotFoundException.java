package com.userfront.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AppointmentNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AppointmentNotFoundException(Long id) {
        super("Appointment not found: " + id);
    }
}
