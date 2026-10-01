package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
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
import com.userfront.service.MoneyAmounts;
import com.userfront.service.TransactionRejectedException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {
	
	private static int nextAccountNumber = 11223145;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private UserService userService;
    
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountLocker accountLocker;

    public PrimaryAccount createPrimaryAccount() {
        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal(0.0));
        primaryAccount.setAccountNumber(accountGen());

        primaryAccountDao.save(primaryAccount);

        return primaryAccountDao.findByAccountNumber(primaryAccount.getAccountNumber());
    }

    public SavingsAccount createSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal(0.0));
        savingsAccount.setAccountNumber(accountGen());

        savingsAccountDao.save(savingsAccount);

        return savingsAccountDao.findByAccountNumber(savingsAccount.getAccountNumber());
    }
    
    @Transactional
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal validAmount = MoneyAmounts.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = accountLocker.lock(user.getPrimaryAccount());
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(validAmount));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", validAmount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = accountLocker.lock(user.getSavingsAccount());
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(validAmount));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", validAmount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new TransactionRejectedException("Please select a valid account.");
        }
    }
    
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal validAmount = MoneyAmounts.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = accountLocker.lock(user.getPrimaryAccount());
            MoneyAmounts.requireSufficientFunds(primaryAccount.getAccountBalance(), validAmount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(validAmount));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", validAmount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = accountLocker.lock(user.getSavingsAccount());
            MoneyAmounts.requireSufficientFunds(savingsAccount.getAccountBalance(), validAmount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(validAmount));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", validAmount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new TransactionRejectedException("Please select a valid account.");
        }
    }
    
    private int accountGen() {
        return ++nextAccountNumber;
    }

	

}
