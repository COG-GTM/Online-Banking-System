package com.userfront.resource.dto;

import java.util.Date;

public class AppointmentSummary {

    private final Long id;
    private final Date date;
    private final String location;
    private final String description;
    private final boolean confirmed;
    private final String username;

    public AppointmentSummary(Long id, Date date, String location, String description, boolean confirmed, String username) {
        this.id = id;
        this.date = date;
        this.location = location;
        this.description = description;
        this.confirmed = confirmed;
        this.username = username;
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

    public String getUsername() {
        return username;
    }
}
