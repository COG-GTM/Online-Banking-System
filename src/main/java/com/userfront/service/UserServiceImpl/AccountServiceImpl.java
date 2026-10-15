package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.function.Function;

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

    private static final int MAX_BALANCE_UPDATE_ATTEMPTS = 10;

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
            BigDecimal balance = adjustBalance(primaryAccountDao::findBalanceById, primaryAccountDao::compareAndSetBalance, primaryAccount.getId(), value);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", value.doubleValue(), balance, primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
        } else if (isSavings(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            BigDecimal balance = adjustBalance(savingsAccountDao::findBalanceById, savingsAccountDao::compareAndSetBalance, savingsAccount.getId(), value);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Deposit to savings Account", "Account", "Finished", value.doubleValue(), balance, savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw unknownAccountType();
        }
    }

    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = TransactionAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if (isPrimary(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            BigDecimal balance = adjustBalance(primaryAccountDao::findBalanceById, primaryAccountDao::compareAndSetBalance, primaryAccount.getId(), value.negate());

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Withdraw from Primary Account", "Account", "Finished", value.doubleValue(), balance, primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (isSavings(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            BigDecimal balance = adjustBalance(savingsAccountDao::findBalanceById, savingsAccountDao::compareAndSetBalance, savingsAccount.getId(), value.negate());

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Withdraw from savings Account", "Account", "Finished", value.doubleValue(), balance, savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw unknownAccountType();
        }
    }

    /*
     * Compare-and-set on the balance column: each UPDATE is atomic on its own (the MySQL5Dialect
     * tables are MyISAM, so there are no row locks), it can never drive the balance negative, and
     * the balance it writes is exactly the one recorded in this operation's ledger row.
     */
    private static BigDecimal adjustBalance(Function<Long, BigDecimal> balanceOf, BalanceSwap swap, Long accountId, BigDecimal delta) {
        for (int attempt = 0; attempt < MAX_BALANCE_UPDATE_ATTEMPTS; attempt++) {
            BigDecimal current = balanceOf.apply(accountId);
            if (current == null) {
                throw new IllegalStateException("Account not found.");
            }
            BigDecimal updated = current.add(delta);
            if (updated.signum() < 0) {
                throw new InsufficientFundsException();
            }
            if (swap.compareAndSet(accountId, current, updated) == 1) {
                return updated;
            }
        }
        throw new InvalidTransactionException("The account is busy. Please try again.");
    }

    @FunctionalInterface
    private interface BalanceSwap {
        int compareAndSet(Long accountId, BigDecimal expected, BigDecimal updated);
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
