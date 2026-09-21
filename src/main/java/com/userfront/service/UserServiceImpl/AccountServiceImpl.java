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
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final int ACCOUNT_NUMBER_LOWER_BOUND = 100000000;
    private static final int ACCOUNT_NUMBER_UPPER_BOUND = 999999999;

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
    
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", amount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);

        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Deposit to savings Account", "Account", "Finished", amount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Unknown account type " + accountType);
        }
    }
    
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            requireSufficientFunds(primaryAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Withdraw from Primary Account", "Account", "Finished", amount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            requireSufficientFunds(savingsAccount.getAccountBalance(), amount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Withdraw from savings Account", "Account", "Finished", amount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Unknown account type " + accountType);
        }
    }

    private void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for this withdrawal");
        }
    }

    private int accountGen() {
        int accountNumber;
        do {
            accountNumber = ACCOUNT_NUMBER_LOWER_BOUND + random.nextInt(ACCOUNT_NUMBER_UPPER_BOUND - ACCOUNT_NUMBER_LOWER_BOUND + 1);
        } while (primaryAccountDao.existsByAccountNumber(accountNumber) || savingsAccountDao.existsByAccountNumber(accountNumber));

        return accountNumber;
    }
}
