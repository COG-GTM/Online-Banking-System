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
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.InvalidTransactionException;
import com.userfront.service.TransactionAmount;
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
        BigDecimal value = TransactionAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if (isPrimary(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            requireUpdated(primaryAccountDao.credit(primaryAccount.getId(), value));
            BigDecimal balance = primaryAccountDao.findBalanceById(primaryAccount.getId());

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", value.doubleValue(), balance, primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
        } else if (isSavings(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            requireUpdated(savingsAccountDao.credit(savingsAccount.getId(), value));
            BigDecimal balance = savingsAccountDao.findBalanceById(savingsAccount.getId());

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Deposit to savings Account", "Account", "Finished", value.doubleValue(), balance, savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw unknownAccountType();
        }
    }

    /*
     * The debit is a single conditional UPDATE (balance >= amount), so the check and the update
     * are atomic in the database even without row locks (the MySQL5Dialect tables are MyISAM).
     */
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = TransactionAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if (isPrimary(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            requireCovered(primaryAccountDao.debitIfCovered(primaryAccount.getId(), value));
            BigDecimal balance = primaryAccountDao.findBalanceById(primaryAccount.getId());

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Withdraw from Primary Account", "Account", "Finished", value.doubleValue(), balance, primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (isSavings(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            requireCovered(savingsAccountDao.debitIfCovered(savingsAccount.getId(), value));
            BigDecimal balance = savingsAccountDao.findBalanceById(savingsAccount.getId());

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Withdraw from savings Account", "Account", "Finished", value.doubleValue(), balance, savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw unknownAccountType();
        }
    }

    private static void requireUpdated(int rows) {
        if (rows != 1) {
            throw new IllegalStateException("Account not found.");
        }
    }

    private static void requireCovered(int rows) {
        if (rows != 1) {
            throw new InsufficientFundsException();
        }
    }

    private static boolean isPrimary(String accountType) {
        return "Primary".equalsIgnoreCase(accountType);
    }

    private static boolean isSavings(String accountType) {
        return "Savings".equalsIgnoreCase(accountType);
    }

    private static InvalidTransactionException unknownAccountType() {
        return new InvalidTransactionException("Please select the Primary or Savings account.");
    }

    private int accountGen() {
        return ++nextAccountNumber;
    }

	

}
