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

        if (BalanceUpdates.isPrimary(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            BigDecimal balance = BalanceUpdates.adjust(primaryAccountDao::findBalanceById, primaryAccountDao::compareAndSetBalance, primaryAccount.getId(), value);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Deposit to Primary Account", "Account", "Finished", value.doubleValue(), balance, primaryAccount);
            transactionService.savePrimaryDepositTransaction(primaryTransaction);
        } else if (BalanceUpdates.isSavings(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            BigDecimal balance = BalanceUpdates.adjust(savingsAccountDao::findBalanceById, savingsAccountDao::compareAndSetBalance, savingsAccount.getId(), value);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Deposit to savings Account", "Account", "Finished", value.doubleValue(), balance, savingsAccount);
            transactionService.saveSavingsDepositTransaction(savingsTransaction);
        } else {
            throw BalanceUpdates.unknownAccountType();
        }
    }

    @Transactional
    public void withdraw(String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = TransactionAmount.requireValid(amount);
        User user = userService.findByUsername(principal.getName());

        if (BalanceUpdates.isPrimary(accountType)) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            BigDecimal balance = BalanceUpdates.adjust(primaryAccountDao::findBalanceById, primaryAccountDao::compareAndSetBalance, primaryAccount.getId(), value.negate());

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Withdraw from Primary Account", "Account", "Finished", value.doubleValue(), balance, primaryAccount);
            transactionService.savePrimaryWithdrawTransaction(primaryTransaction);
        } else if (BalanceUpdates.isSavings(accountType)) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            BigDecimal balance = BalanceUpdates.adjust(savingsAccountDao::findBalanceById, savingsAccountDao::compareAndSetBalance, savingsAccount.getId(), value.negate());

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Withdraw from savings Account", "Account", "Finished", value.doubleValue(), balance, savingsAccount);
            transactionService.saveSavingsWithdrawTransaction(savingsTransaction);
        } else {
            throw BalanceUpdates.unknownAccountType();
        }
    }

    private int accountGen() {
        return ++nextAccountNumber;
    }

	

}
