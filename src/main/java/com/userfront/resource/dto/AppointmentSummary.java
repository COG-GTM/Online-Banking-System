package com.userfront.resource.dto;

import java.util.Date;

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

    public AppointmentSummary(Long id, Date date, String location, String description, boolean confirmed,
                              Long userId, String username, String firstName, String lastName) {
        this.id = id;
        this.date = date;
        this.location = location;
        this.description = description;
        this.confirmed = confirmed;
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
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
