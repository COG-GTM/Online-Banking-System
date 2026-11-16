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
import com.userfront.service.TransactionAmount;
import com.userfront.service.TransactionDeclinedException;
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

        if ("Primary".equalsIgnoreCase(accountType)) {
            Long accountId = user.getPrimaryAccount().getId();
            requireUpdated(primaryAccountDao.credit(accountId, value));
            PrimaryAccount primaryAccount = primaryAccountDao.findById(accountId).get();

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", value.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            Long accountId = user.getSavingsAccount().getId();
            requireUpdated(savingsAccountDao.credit(accountId, value));
            SavingsAccount savingsAccount = savingsAccountDao.findById(accountId).get();

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", value.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new TransactionDeclinedException("Select an account.");
        }
    }
    
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = TransactionAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            Long accountId = user.getPrimaryAccount().getId();
            if (primaryAccountDao.debitIfSufficient(accountId, value) != 1) {
                throw new InsufficientFundsException();
            }
            PrimaryAccount primaryAccount = primaryAccountDao.findById(accountId).get();

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", value.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            Long accountId = user.getSavingsAccount().getId();
            if (savingsAccountDao.debitIfSufficient(accountId, value) != 1) {
                throw new InsufficientFundsException();
            }
            SavingsAccount savingsAccount = savingsAccountDao.findById(accountId).get();

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", value.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new TransactionDeclinedException("Select an account.");
        }
    }

    private static void requireUpdated(int rows) {
        if (rows != 1) {
            throw new IllegalStateException("Account not found");
        }
    }
    
    private int accountGen() {
        return ++nextAccountNumber;
    }

	

}
