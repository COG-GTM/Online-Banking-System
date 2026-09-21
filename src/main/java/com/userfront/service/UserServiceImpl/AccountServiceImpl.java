package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.AccountService;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {

    private static final int ACCOUNT_NUMBER_ORIGIN = 10_000_000;

    private static final int ACCOUNT_NUMBER_BOUND = 100_000_000;

    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 100;

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionService transactionService;

    public PrimaryAccount createPrimaryAccount() {
        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(BigDecimal.ZERO);
        primaryAccount.setAccountNumber(generatePrimaryAccountNumber());

        primaryAccountDao.save(primaryAccount);

        return primaryAccountDao.findByAccountNumber(primaryAccount.getAccountNumber());
    }

    public SavingsAccount createSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(BigDecimal.ZERO);
        savingsAccount.setAccountNumber(generateSavingsAccountNumber());

        savingsAccountDao.save(savingsAccount);

        return savingsAccountDao.findByAccountNumber(savingsAccount.getAccountNumber());
    }

    @Transactional
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = primaryAccountDao.findForUpdate(user.getPrimaryAccount().getId())
                    .orElseThrow(() -> new IllegalStateException("Primary account not found"));
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = savingsAccountDao.findForUpdate(user.getSavingsAccount().getId())
                    .orElseThrow(() -> new IllegalStateException("Savings account not found"));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Deposit to savings Account", "Account", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = primaryAccountDao.findForUpdate(user.getPrimaryAccount().getId())
                    .orElseThrow(() -> new IllegalStateException("Primary account not found"));
            if (primaryAccount.getAccountBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds in primary account");
            }
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Withdraw from Primary Account", "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = savingsAccountDao.findForUpdate(user.getSavingsAccount().getId())
                    .orElseThrow(() -> new IllegalStateException("Savings account not found"));
            if (savingsAccount.getAccountBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds in savings account");
            }
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Withdraw from savings Account", "Account", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

    private int generatePrimaryAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int candidate = randomAccountNumber();
            if (!primaryAccountDao.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }

    private int generateSavingsAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int candidate = randomAccountNumber();
            if (!savingsAccountDao.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }

    private int randomAccountNumber() {
        return ACCOUNT_NUMBER_ORIGIN + random.nextInt(ACCOUNT_NUMBER_BOUND - ACCOUNT_NUMBER_ORIGIN);
    }
}
