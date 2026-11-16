package com.userfront.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.Transient;

import org.springframework.data.domain.Persistable;

@Entity
public class AccountNumberSequence implements Persistable<String> {

    @Id
    @Column(length = 64)
    private String name;

    @Column(nullable = false)
    private int nextValue;

    // New rows must be inserted, never merged over a row another signup created first.
    @Transient
    private boolean isNew = true;

    protected AccountNumberSequence() {
    }

    public AccountNumberSequence(String name, int nextValue) {
        this.name = name;
        this.nextValue = nextValue;
    }

    @Override
    public String getId() {
        return name;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }

    public String getName() {
        return name;
    }

    public int getNextValue() {
        return nextValue;
    }

    public void setNextValue(int nextValue) {
        this.nextValue = nextValue;
    }
}
