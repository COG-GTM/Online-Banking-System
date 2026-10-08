package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

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
import com.userfront.service.MoneyAmount;
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

    @PersistenceContext
    private EntityManager entityManager;

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
    
    /*
     * Ledger rows are saved before the account row is locked: their ids come from hibernate_sequence,
     * which Hibernate reads on a second pooled connection. Allocating that id while holding the row
     * lock lets concurrent requests (each holding a connection and waiting on the lock) exhaust the pool.
     */
    @Transactional
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = MoneyAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());
        Date date = new Date();

        if (isPrimary(accountType)) {
            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", value.doubleValue(), null, user.getPrimaryAccount());
            transactionService.savePrimaryDepositTransaction(primaryTransaction);

            PrimaryAccount primaryAccount = lockForUpdate(PrimaryAccount.class, user.getPrimaryAccount().getId());
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(value));
            primaryAccountDao.save(primaryAccount);
            primaryTransaction.setAvailableBalance(primaryAccount.getAccountBalance());
        } else if (isSavings(accountType)) {
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", value.doubleValue(), null, user.getSavingsAccount());
            transactionService.saveSavingsDepositTransaction(savingsTransaction);

            SavingsAccount savingsAccount = lockForUpdate(SavingsAccount.class, user.getSavingsAccount().getId());
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(value));
            savingsAccountDao.save(savingsAccount);
            savingsTransaction.setAvailableBalance(savingsAccount.getAccountBalance());
        } else {
            throw unknownAccountType();
        }
    }
    
    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = MoneyAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());
        Date date = new Date();

        if (isPrimary(accountType)) {
            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", value.doubleValue(), null, user.getPrimaryAccount());
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);

            PrimaryAccount primaryAccount = lockForUpdate(PrimaryAccount.class, user.getPrimaryAccount().getId());
            requireFunds(primaryAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(value));
            primaryAccountDao.save(primaryAccount);
            primaryTransaction.setAvailableBalance(primaryAccount.getAccountBalance());
        } else if (isSavings(accountType)) {
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", value.doubleValue(), null, user.getSavingsAccount());
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);

            SavingsAccount savingsAccount = lockForUpdate(SavingsAccount.class, user.getSavingsAccount().getId());
            requireFunds(savingsAccount.getAccountBalance(), value);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(value));
            savingsAccountDao.save(savingsAccount);
            savingsTransaction.setAvailableBalance(savingsAccount.getAccountBalance());
        } else {
            throw unknownAccountType();
        }
    }

    /**
     * Re-reads the account row with SELECT ... FOR UPDATE so the balance is current and no
     * concurrent deposit/withdrawal can change it before this transaction commits.
     */
    private <T> T lockForUpdate(Class<T> type, Long id) {
        T account = entityManager.find(type, id);
        if (account == null) {
            throw new IllegalStateException(type.getSimpleName() + " " + id + " not found");
        }
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }

    private static void requireFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds: the amount exceeds the available balance.");
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
