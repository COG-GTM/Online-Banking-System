package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.service.TransactionDeclinedException;

/**
 * Applies balance changes with single conditional UPDATE statements so a debit
 * can never take an account below zero, even under concurrent requests.
 */
@Component
public class AccountLedger {

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    public static BigDecimal validAmount(String raw) {
        if (raw == null) {
            throw new TransactionDeclinedException("Enter a valid amount.");
        }
        try {
            return validAmount(new BigDecimal(raw.trim()));
        } catch (NumberFormatException e) {
            throw new TransactionDeclinedException("Enter a valid amount.");
        }
    }

    public static BigDecimal validAmount(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw)) {
            throw new TransactionDeclinedException("Enter a valid amount.");
        }
        return validAmount(BigDecimal.valueOf(raw));
    }

    private static BigDecimal validAmount(BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new TransactionDeclinedException("Amount must be greater than zero.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new TransactionDeclinedException("Amount cannot have more than two decimal places.");
        }
        return amount.setScale(2);
    }

    /** @return true if the balance covered the amount and was debited, false if nothing changed. */
    public boolean debit(PrimaryAccount account, BigDecimal amount) {
        return primaryAccountDao.debitIfSufficient(account.getId(), amount) == 1;
    }

    /** @return true if the balance covered the amount and was debited, false if nothing changed. */
    public boolean debit(SavingsAccount account, BigDecimal amount) {
        return savingsAccountDao.debitIfSufficient(account.getId(), amount) == 1;
    }

    public void credit(PrimaryAccount account, BigDecimal amount) {
        if (primaryAccountDao.credit(account.getId(), amount) != 1) {
            throw new IllegalStateException("Primary account " + account.getId() + " not found");
        }
    }

    public void credit(SavingsAccount account, BigDecimal amount) {
        if (savingsAccountDao.credit(account.getId(), amount) != 1) {
            throw new IllegalStateException("Savings account " + account.getId() + " not found");
        }
    }

    public PrimaryAccount reload(PrimaryAccount account) {
        return primaryAccountDao.findById(account.getId())
                .orElseThrow(() -> new IllegalStateException("Primary account " + account.getId() + " not found"));
    }

    public SavingsAccount reload(SavingsAccount account) {
        return savingsAccountDao.findById(account.getId())
                .orElseThrow(() -> new IllegalStateException("Savings account " + account.getId() + " not found"));
    }
}
