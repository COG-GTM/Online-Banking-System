package com.userfront.resource.dto;

import com.userfront.domain.User;

public class UserSummary {

    private final Long userId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final boolean enabled;
    private final AccountSummary primaryAccount;
    private final AccountSummary savingsAccount;

    private UserSummary(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.enabled = user.isEnabled();
        this.primaryAccount = AccountSummary.from(user.getPrimaryAccount());
        this.savingsAccount = AccountSummary.from(user.getSavingsAccount());
    }

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user);
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

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AccountSummary getPrimaryAccount() {
        return primaryAccount;
    }

    public AccountSummary getSavingsAccount() {
        return savingsAccount;
    }
}
