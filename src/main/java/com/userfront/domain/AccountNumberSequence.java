package com.userfront.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AccountNumberSequence {

    @Id
    private String name;
    private int lastIssued;

    public AccountNumberSequence() {
    }

    public AccountNumberSequence(String name, int lastIssued) {
        this.name = name;
        this.lastIssued = lastIssued;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLastIssued() {
        return lastIssued;
    }

    public void setLastIssued(int lastIssued) {
        this.lastIssued = lastIssued;
    }
}
