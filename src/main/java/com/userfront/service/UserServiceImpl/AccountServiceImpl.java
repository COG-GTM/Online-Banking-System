package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.security.SecureRandom;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.AccountService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ACCOUNT_NUMBER_ORIGIN = 100000000;
    private static final int ACCOUNT_NUMBER_BOUND = 900000000;
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 20;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionService transactionService;

    public PrimaryAccount createPrimaryAccount() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int accountNumber = accountGen();
            if (isAccountNumberTaken(accountNumber)) {
                continue;
            }

            PrimaryAccount primaryAccount = new PrimaryAccount();
            primaryAccount.setAccountBalance(new BigDecimal(0.0));
            primaryAccount.setAccountNumber(accountNumber);

            try {
                primaryAccountDao.save(primaryAccount);
            } catch (DataIntegrityViolationException e) {
                continue;
            }

            return primaryAccountDao.findByAccountNumber(accountNumber);
        }

        throw new IllegalStateException("Unable to allocate a unique primary account number");
    }

    public SavingsAccount createSavingsAccount() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int accountNumber = accountGen();
            if (isAccountNumberTaken(accountNumber)) {
                continue;
            }

            SavingsAccount savingsAccount = new SavingsAccount();
            savingsAccount.setAccountBalance(new BigDecimal(0.0));
            savingsAccount.setAccountNumber(accountNumber);

            try {
                savingsAccountDao.save(savingsAccount);
            } catch (DataIntegrityViolationException e) {
                continue;
            }

            return savingsAccountDao.findByAccountNumber(accountNumber);
        }

        throw new IllegalStateException("Unable to allocate a unique savings account number");
    }
    
    public void deposit(String accountType, double amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(new BigDecimal(amount)));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(new BigDecimal(amount)));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        }
    }
    
    public void withdraw(String accountType, double amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(new BigDecimal(amount)));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(new BigDecimal(amount)));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        }
    }
    
    private int accountGen() {
        return ACCOUNT_NUMBER_ORIGIN + RANDOM.nextInt(ACCOUNT_NUMBER_BOUND);
    }

    private boolean isAccountNumberTaken(int accountNumber) {
        return primaryAccountDao.findByAccountNumber(accountNumber) != null
                || savingsAccountDao.findByAccountNumber(accountNumber) != null;
    }

	

}
