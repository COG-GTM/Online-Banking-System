package com.userfront.domain;

import java.util.Optional;

public enum AccountType {
    PRIMARY("Primary"),
    SAVINGS("Savings");

    private final String value;

    AccountType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<AccountType> fromValue(String value) {
        for (AccountType type : values()) {
            if (type.value.equals(value)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
