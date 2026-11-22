package com.userfront.dto;

import com.userfront.domain.User;

public final class ProfileView {

    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final Integer primaryAccountNumber;
    private final Integer savingsAccountNumber;

    private ProfileView(User user) {
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.primaryAccountNumber = user.getPrimaryAccount() == null ? null : user.getPrimaryAccount().getAccountNumber();
        this.savingsAccountNumber = user.getSavingsAccount() == null ? null : user.getSavingsAccount().getAccountNumber();
    }

    public static ProfileView from(User user) {
        return new ProfileView(user);
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Integer getPrimaryAccountNumber() {
        return primaryAccountNumber;
    }

    public Integer getSavingsAccountNumber() {
        return savingsAccountNumber;
    }
}
