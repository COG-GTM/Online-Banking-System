package com.userfront.resource.dto;

import java.math.BigDecimal;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;

public class AdminUserView {

    private final Long userId;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final boolean enabled;
    private final AccountSummary primaryAccount;
    private final AccountSummary savingsAccount;

    private AdminUserView(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.enabled = user.isEnabled();
        this.primaryAccount = AccountSummary.of(user.getPrimaryAccount());
        this.savingsAccount = AccountSummary.of(user.getSavingsAccount());
    }

    public static AdminUserView from(User user) {
        return new AdminUserView(user);
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

    public static class AccountSummary {

        private final int accountNumber;
        private final BigDecimal accountBalance;

        private AccountSummary(int accountNumber, BigDecimal accountBalance) {
            this.accountNumber = accountNumber;
            this.accountBalance = accountBalance;
        }

        static AccountSummary of(PrimaryAccount account) {
            return account == null ? null : new AccountSummary(account.getAccountNumber(), account.getAccountBalance());
        }

        static AccountSummary of(SavingsAccount account) {
            return account == null ? null : new AccountSummary(account.getAccountNumber(), account.getAccountBalance());
        }

        public int getAccountNumber() {
            return accountNumber;
        }

        public BigDecimal getAccountBalance() {
            return accountBalance;
        }
    }
}
