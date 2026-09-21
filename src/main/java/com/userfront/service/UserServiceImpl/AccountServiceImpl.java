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
import com.userfront.service.Amounts;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {
	
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int ACCOUNT_NUMBER_ORIGIN = 10_000_000;
	private static final int ACCOUNT_NUMBER_BOUND = 90_000_000;

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
        BigDecimal depositAmount = Amounts.validatePositive(amount);
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(depositAmount));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", depositAmount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(depositAmount));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", depositAmount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
    }
    
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal withdrawAmount = Amounts.validatePositive(amount);
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            requireSufficientFunds(primaryAccount.getAccountBalance(), withdrawAmount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(withdrawAmount));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", withdrawAmount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            requireSufficientFunds(savingsAccount.getAccountBalance(), withdrawAmount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(withdrawAmount));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", withdrawAmount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Unknown account type: " + accountType);
        }
    }

    private static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds for this transaction");
        }
    }

    private int accountGen() {
        int accountNumber;
        do {
            accountNumber = ACCOUNT_NUMBER_ORIGIN + RANDOM.nextInt(ACCOUNT_NUMBER_BOUND);
        } while (primaryAccountDao.findByAccountNumber(accountNumber) != null
                || savingsAccountDao.findByAccountNumber(accountNumber) != null);

        return accountNumber;
    }

	

}
