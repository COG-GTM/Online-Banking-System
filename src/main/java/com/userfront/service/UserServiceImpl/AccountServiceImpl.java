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
import com.userfront.util.MoneyUtil;

@Service
public class AccountServiceImpl implements AccountService {
	
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final int ACCOUNT_NUMBER_ORIGIN = 100_000_000;
	private static final int ACCOUNT_NUMBER_BOUND = 900_000_000;
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
    public void deposit(String accountType, String amount, Principal principal) {
        BigDecimal depositAmount = MoneyUtil.parsePositiveAmount(amount);
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = primaryAccountDao.findLockedById(user.getPrimaryAccount().getId());
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(depositAmount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", depositAmount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);

        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = savingsAccountDao.findLockedById(user.getSavingsAccount().getId());
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(depositAmount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Deposit to savings Account", "Account", "Finished", depositAmount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }
    
    @Transactional
    public void withdraw(String accountType, String amount, Principal principal) {
        BigDecimal withdrawAmount = MoneyUtil.parsePositiveAmount(amount);
        User user = userService.findByUsername(principal.getName());

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = primaryAccountDao.findLockedById(user.getPrimaryAccount().getId());
            requireSufficientFunds(primaryAccount.getAccountBalance(), withdrawAmount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(withdrawAmount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Withdraw from Primary Account", "Account", "Finished", withdrawAmount, primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = savingsAccountDao.findLockedById(user.getSavingsAccount().getId());
            requireSufficientFunds(savingsAccount.getAccountBalance(), withdrawAmount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(withdrawAmount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Withdraw from savings Account", "Account", "Finished", withdrawAmount, savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

    private void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }
    
    private int accountGen() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int candidate = ACCOUNT_NUMBER_ORIGIN + RANDOM.nextInt(ACCOUNT_NUMBER_BOUND);
            if (!primaryAccountDao.existsByAccountNumber(candidate) && !savingsAccountDao.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique account number");
    }

	

}
