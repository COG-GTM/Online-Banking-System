package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.userfront.service.AmountValidator;
import com.userfront.service.InvalidTransactionException;
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
    
    @Transactional
    public void deposit(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal validAmount = AmountValidator.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = lockForUpdate(user.getPrimaryAccount());
            primaryAccountDao.credit(primaryAccount.getId(), validAmount);
            entityManager.refresh(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Deposit to Primary Account", "Account", "Finished", validAmount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);

        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = lockForUpdate(user.getSavingsAccount());
            savingsAccountDao.credit(savingsAccount.getId(), validAmount);
            entityManager.refresh(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Deposit to savings Account", "Account", "Finished", validAmount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw new InvalidTransactionException("Please select a valid account.");
        }
    }

    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal validAmount = AmountValidator.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = lockForUpdate(user.getPrimaryAccount());
            requireSufficientFunds(primaryAccount.getAccountBalance(), validAmount);
            if (primaryAccountDao.debitIfSufficient(primaryAccount.getId(), validAmount) != 1) {
                throw insufficientFunds(primaryAccount.getAccountBalance());
            }
            entityManager.refresh(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Withdraw from Primary Account", "Account", "Finished", validAmount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = lockForUpdate(user.getSavingsAccount());
            requireSufficientFunds(savingsAccount.getAccountBalance(), validAmount);
            if (savingsAccountDao.debitIfSufficient(savingsAccount.getId(), validAmount) != 1) {
                throw insufficientFunds(savingsAccount.getAccountBalance());
            }
            entityManager.refresh(savingsAccount);

            Date date = new Date();
            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Withdraw from savings Account", "Account", "Finished", validAmount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw new InvalidTransactionException("Please select a valid account.");
        }
    }

    /**
     * Re-reads the account with SELECT ... FOR UPDATE so concurrent updates on the row wait for this
     * transaction. The balance change itself is a single conditional UPDATE, so funds are enforced
     * atomically even where row locks are not honoured (e.g. legacy MyISAM tables).
     */
    private <T> T lockForUpdate(T account) {
        if (account == null) {
            throw new InvalidTransactionException("Please select a valid account.");
        }
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }

    private static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw insufficientFunds(balance);
        }
    }

    private static InvalidTransactionException insufficientFunds(BigDecimal balance) {
        return new InvalidTransactionException("Insufficient funds: the available balance is $" + balance.setScale(2, RoundingMode.HALF_UP).toPlainString() + ".");
    }

    private int accountGen() {
        return ++nextAccountNumber;
    }

	

}
