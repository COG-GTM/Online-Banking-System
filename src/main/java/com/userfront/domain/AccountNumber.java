package com.userfront.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AccountNumber {

    @Id
    private int accountNumber;

    public AccountNumber() {
    }

    public AccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

}
