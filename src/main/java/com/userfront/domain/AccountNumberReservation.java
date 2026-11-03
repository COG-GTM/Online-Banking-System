package com.userfront.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "account_number_reservation")
public class AccountNumberReservation {

    @Id
    private int accountNumber;

    private Date reservedAt;

    protected AccountNumberReservation() {
    }

    public AccountNumberReservation(int accountNumber) {
        this.accountNumber = accountNumber;
        this.reservedAt = new Date();
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public Date getReservedAt() {
        return reservedAt;
    }
}
