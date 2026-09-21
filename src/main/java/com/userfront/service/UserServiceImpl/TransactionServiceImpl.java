package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private PrimaryTransactionDao primaryTransactionDao;
	
	@Autowired
	private SavingsTransactionDao savingsTransactionDao;
	
	@Autowired
	private PrimaryAccountDao primaryAccountDao;
	
	@Autowired
	private SavingsAccountDao savingsAccountDao;
	
	@Autowired
	private RecipientDao recipientDao;
	

	public List<PrimaryTransaction> findPrimaryTransactionList(String username){
        User user = userService.findByUsername(username);
        List<PrimaryTransaction> primaryTransactionList = user.getPrimaryAccount().getPrimaryTransactionList();

        return primaryTransactionList;
    }

    public List<SavingsTransaction> findSavingsTransactionList(String username) {
        User user = userService.findByUsername(username);
        List<SavingsTransaction> savingsTransactionList = user.getSavingsAccount().getSavingsTransactionList();

        return savingsTransactionList;
    }

    public void savePrimaryDepositTransaction(PrimaryTransaction primaryTransaction) {
        primaryTransactionDao.save(primaryTransaction);
    }

    public void saveSavingsDepositTransaction(SavingsTransaction savingsTransaction) {
        savingsTransactionDao.save(savingsTransaction);
    }
    
    public void savePrimaryWithdrawTransaction(PrimaryTransaction primaryTransaction) {
        primaryTransactionDao.save(primaryTransaction);
    }

    public void saveSavingsWithdrawTransaction(SavingsTransaction savingsTransaction) {
        savingsTransactionDao.save(savingsTransaction);
    }
    
    public void betweenAccountsTransfer(String transferFrom, String transferTo, BigDecimal amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if ("Primary".equalsIgnoreCase(transferFrom) && "Savings".equalsIgnoreCase(transferTo)) {
            requireSufficientFunds(primaryAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", amount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(transferFrom) && "Primary".equalsIgnoreCase(transferTo)) {
            requireSufficientFunds(savingsAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", amount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid Transfer");
        }
    }
    
    public List<Recipient> findRecipientList(Principal principal) {
        return recipientDao.findByUserUsername(principal.getName());
    }

    public Recipient saveRecipient(Recipient recipient) {
        return recipientDao.save(recipient);
    }

    public Recipient findRecipientByName(String recipientName, String username) {
        return recipientDao.findByNameAndUserUsername(recipientName, username);
    }

    public void deleteRecipientByName(String recipientName, String username) {
        recipientDao.deleteByNameAndUserUsername(recipientName, username);
    }
    
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, BigDecimal amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if ("Primary".equalsIgnoreCase(accountType)) {
            requireSufficientFunds(primaryAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            requireSufficientFunds(savingsAccount.getAccountBalance(), amount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Unknown account type " + accountType);
        }
    }

    private void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance == null || balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds for this transfer");
        }
    }
}
