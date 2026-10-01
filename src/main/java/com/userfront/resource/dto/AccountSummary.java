package com.userfront.resource.dto;

import java.math.BigDecimal;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

public class AccountSummary {

    private final int accountNumber;
    private final BigDecimal accountBalance;

    public AccountSummary(int accountNumber, BigDecimal accountBalance) {
        this.accountNumber = accountNumber;
        this.accountBalance = accountBalance;
    }

    public static AccountSummary from(PrimaryAccount account) {
        return account == null ? null : new AccountSummary(account.getAccountNumber(), account.getAccountBalance());
    }

    public static AccountSummary from(SavingsAccount account) {
        return account == null ? null : new AccountSummary(account.getAccountNumber(), account.getAccountBalance());
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }
}
