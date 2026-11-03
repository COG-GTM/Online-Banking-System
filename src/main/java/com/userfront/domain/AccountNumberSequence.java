package com.userfront.domain;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class AccountNumberSequence {

    @Id
    private String name;
    private int lastValue;

    public AccountNumberSequence() {
    }

    public AccountNumberSequence(String name, int lastValue) {
        this.name = name;
        this.lastValue = lastValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLastValue() {
        return lastValue;
    }

    public void setLastValue(int lastValue) {
        this.lastValue = lastValue;
    }
}
