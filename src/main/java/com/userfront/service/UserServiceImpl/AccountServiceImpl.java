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
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {
	
	private static final int MIN_ACCOUNT_NUMBER = 10000000;
	private static final int MAX_ACCOUNT_NUMBER = 99999999;
	private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 20;

	private final SecureRandom secureRandom = new SecureRandom();

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
        primaryAccount.setAccountNumber(accountGen());

        primaryAccountDao.save(primaryAccount);

        return primaryAccountDao.findByAccountNumber(primaryAccount.getAccountNumber());
    }

    public SavingsAccount createSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(BigDecimal.ZERO);
        savingsAccount.setAccountNumber(accountGen());

        savingsAccountDao.save(savingsAccount);

        return savingsAccountDao.findByAccountNumber(savingsAccount.getAccountNumber());
    }
    
    @Transactional
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Date date = new Date();

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = primaryAccountDao.findWithLockByAccountNumber(user.getPrimaryAccount().getAccountNumber());
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = savingsAccountDao.findWithLockByAccountNumber(user.getSavingsAccount().getAccountNumber());
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }
    
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Date date = new Date();

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = primaryAccountDao.findWithLockByAccountNumber(user.getPrimaryAccount().getAccountNumber());
            requireSufficientFunds(primaryAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = savingsAccountDao.findWithLockByAccountNumber(user.getSavingsAccount().getAccountNumber());
            requireSufficientFunds(savingsAccount.getAccountBalance(), amount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

    private void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }
    
    private int accountGen() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int candidate = MIN_ACCOUNT_NUMBER + secureRandom.nextInt(MAX_ACCOUNT_NUMBER - MIN_ACCOUNT_NUMBER + 1);
            if (primaryAccountDao.findByAccountNumber(candidate) == null && savingsAccountDao.findByAccountNumber(candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique account number");
    }

	

}
