package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {
	
	private static int nextAccountNumber = 11223145;

    private static final int AMOUNT_SCALE = 2;
    private static final int MAX_AMOUNT_INPUT_LENGTH = 32;
    private static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");

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
        BigDecimal validAmount = validateAmount(amount);
        User user = userService.findByUsername(principal.getName());
        Date date = new Date();

        // Ledger rows are saved before the balance update so their ids are allocated before the account row is locked.
        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", validAmount.doubleValue(), null, primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
            if (primaryAccountDao.creditBalance(primaryAccount.getId(), validAmount) != 1) {
                throw new IllegalStateException("Primary account not found.");
            }
            primaryTransaction.setAvailableBalance(primaryAccountDao.findBalanceById(primaryAccount.getId()));
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", validAmount.doubleValue(), null, savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
            if (savingsAccountDao.creditBalance(savingsAccount.getId(), validAmount) != 1) {
                throw new IllegalStateException("Savings account not found.");
            }
            savingsTransaction.setAvailableBalance(savingsAccountDao.findBalanceById(savingsAccount.getId()));
        } else {
            throw new IllegalArgumentException("Unknown account type.");
        }
    }
    
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal validAmount = validateAmount(amount);
        User user = userService.findByUsername(principal.getName());
        Date date = new Date();

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", validAmount.doubleValue(), null, primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
            if (primaryAccountDao.debitBalanceIfSufficient(primaryAccount.getId(), validAmount) != 1) {
                throw new IllegalArgumentException("Insufficient funds.");
            }
            primaryTransaction.setAvailableBalance(primaryAccountDao.findBalanceById(primaryAccount.getId()));
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", validAmount.doubleValue(), null, savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
            if (savingsAccountDao.debitBalanceIfSufficient(savingsAccount.getId(), validAmount) != 1) {
                throw new IllegalArgumentException("Insufficient funds.");
            }
            savingsTransaction.setAvailableBalance(savingsAccountDao.findBalanceById(savingsAccount.getId()));
        } else {
            throw new IllegalArgumentException("Unknown account type.");
        }
    }

    public static BigDecimal parseAmount(String amount) {
        if (amount == null || amount.trim().isEmpty() || amount.trim().length() > MAX_AMOUNT_INPUT_LENGTH) {
            throw new IllegalArgumentException("Please enter a valid amount.");
        }
        try {
            return validateAmount(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid amount.");
        }
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_TRANSACTION_AMOUNT) > 0) {
            throw new IllegalArgumentException("Amount exceeds the maximum allowed per transaction.");
        }
        if (amount.stripTrailingZeros().scale() > AMOUNT_SCALE) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
        }
        return amount.setScale(AMOUNT_SCALE, RoundingMode.UNNECESSARY);
    }

    private int accountGen() {
        return ++nextAccountNumber;
    }

	

}
