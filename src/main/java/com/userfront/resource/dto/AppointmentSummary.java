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
    private final AppointmentUser user;

    private AppointmentSummary(Appointment appointment) {
        this.id = appointment.getId();
        this.date = appointment.getDate();
        this.location = appointment.getLocation();
        this.description = appointment.getDescription();
        this.confirmed = appointment.isConfirmed();
        this.user = AppointmentUser.from(appointment.getUser());
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

    public AppointmentUser getUser() {
        return user;
    }

    public static class AppointmentUser {

        private final Long userId;
        private final String username;
        private final String firstName;
        private final String lastName;

        private AppointmentUser(User user) {
            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.firstName = user.getFirstName();
            this.lastName = user.getLastName();
        }

        static AppointmentUser from(User user) {
            return user == null ? null : new AppointmentUser(user);
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
}
