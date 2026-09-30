package com.userfront.resource.dto;

import java.util.Date;

import com.userfront.domain.Appointment;
import com.userfront.domain.User;

public class AppointmentSummary {

    private final Long id;
    private final Date date;
    private final String location;
    private final String description;
    private final boolean confirmed;
    private final Long userId;
    private final String username;
    private final String firstName;
    private final String lastName;

    private AppointmentSummary(Appointment appointment) {
        User user = appointment.getUser();
        this.id = appointment.getId();
        this.date = appointment.getDate();
        this.location = appointment.getLocation();
        this.description = appointment.getDescription();
        this.confirmed = appointment.isConfirmed();
        this.userId = user == null ? null : user.getUserId();
        this.username = user == null ? null : user.getUsername();
        this.firstName = user == null ? null : user.getFirstName();
        this.lastName = user == null ? null : user.getLastName();
    }

    public static AppointmentSummary from(Appointment appointment) {
        return new AppointmentSummary(appointment);
    }

    public Long getId() {
        return id;
    }

    public Date getDate() {
        return date;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
