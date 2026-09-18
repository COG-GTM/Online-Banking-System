package com.userfront.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * Reservation of an account number. The primary key makes every account number
 * unique across all account types.
 */
@Entity
public class AccountNumberAllocation implements Persistable<Integer> {

    @Id
    private int accountNumber;

    @Transient
    private boolean unsaved = true;

    protected AccountNumberAllocation() {
    }

    public AccountNumberAllocation(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    @Override
    public Integer getId() {
        return accountNumber;
    }

    @Override
    public boolean isNew() {
        return unsaved;
    }
}
