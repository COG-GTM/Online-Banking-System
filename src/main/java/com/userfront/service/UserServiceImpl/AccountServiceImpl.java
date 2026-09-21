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
import com.userfront.exception.InsufficientFundsException;
import com.userfront.exception.InvalidAmountException;
import com.userfront.exception.ResourceNotFoundException;
import com.userfront.service.AccountService;
import com.userfront.service.Amounts;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AccountServiceImpl implements AccountService {

    private static final int ACCOUNT_NUMBER_ORIGIN = 100_000_000;
    private static final int ACCOUNT_NUMBER_BOUND = 1_000_000_000;
    private static final int ACCOUNT_NUMBER_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @Override
    public PrimaryAccount createPrimaryAccount() {
        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(BigDecimal.ZERO.setScale(Amounts.SCALE));
        primaryAccount.setAccountNumber(accountGen());

        return primaryAccountDao.save(primaryAccount);
    }

    @Override
    public SavingsAccount createSavingsAccount() {
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(BigDecimal.ZERO.setScale(Amounts.SCALE));
        savingsAccount.setAccountNumber(accountGen());

        return savingsAccountDao.save(savingsAccount);
    }

    @Override
    @Transactional
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = requirePositive(amount);
        User user = currentUser(principal);

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = lockPrimary(user);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(value));
            primaryAccountDao.save(primaryAccount);

            transactionService.savePrimaryDepositTransaction(new PrimaryTransaction(new Date(),
                    "Deposit to Primary Account", "Account", "Finished", value,
                    primaryAccount.getAccountBalance(), primaryAccount));
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = lockSavings(user);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(value));
            savingsAccountDao.save(savingsAccount);

            transactionService.saveSavingsDepositTransaction(new SavingsTransaction(new Date(),
                    "Deposit to savings Account", "Account", "Finished", value,
                    savingsAccount.getAccountBalance(), savingsAccount));
        } else {
            throw new InvalidAmountException("Unknown account type: " + accountType);
        }
    }

    @Override
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = requirePositive(amount);
        User user = currentUser(principal);

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = lockPrimary(user);
            requireSufficientFunds(primaryAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(value));
            primaryAccountDao.save(primaryAccount);

            transactionService.savePrimaryWithdrawTransaction(new PrimaryTransaction(new Date(),
                    "Withdraw from Primary Account", "Account", "Finished", value,
                    primaryAccount.getAccountBalance(), primaryAccount));
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = lockSavings(user);
            requireSufficientFunds(savingsAccount.getAccountBalance(), value);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(value));
            savingsAccountDao.save(savingsAccount);

            transactionService.saveSavingsWithdrawTransaction(new SavingsTransaction(new Date(),
                    "Withdraw from savings Account", "Account", "Finished", value,
                    savingsAccount.getAccountBalance(), savingsAccount));
        } else {
            throw new InvalidAmountException("Unknown account type: " + accountType);
        }
    }

    private User currentUser(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return user;
    }

    private PrimaryAccount lockPrimary(User user) {
        return primaryAccountDao.findWithLockById(user.getPrimaryAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Primary account not found"));
    }

    private SavingsAccount lockSavings(User user) {
        return savingsAccountDao.findWithLockById(user.getSavingsAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        return amount.setScale(Amounts.SCALE, java.math.RoundingMode.UNNECESSARY);
    }

    private static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient available balance for this transaction");
        }
    }

    private int accountGen() {
        for (int attempt = 0; attempt < ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int candidate = ACCOUNT_NUMBER_ORIGIN + random.nextInt(ACCOUNT_NUMBER_BOUND - ACCOUNT_NUMBER_ORIGIN);
            if (primaryAccountDao.findByAccountNumber(candidate) == null
                    && savingsAccountDao.findByAccountNumber(candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate a unique account number");
    }
}
