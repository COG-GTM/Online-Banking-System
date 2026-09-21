package com.userfront.web;

import com.userfront.domain.User;

public record UserSummary(Long userId, String username, String firstName, String lastName, String email, String phone,
                          boolean enabled) {

    public static UserSummary from(User user) {
        return new UserSummary(user.getUserId(), user.getUsername(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.isEnabled());
    }
}
