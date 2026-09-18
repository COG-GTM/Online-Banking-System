package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
import com.userfront.exception.InvalidTransactionException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
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
    
    @Transactional(rollbackFor = InvalidTransactionException.class)
    public void betweenAccountsTransfer(String transferFrom, String transferTo, BigDecimal amount, Principal principal) throws InvalidTransactionException {
        requirePositive(amount);
        User user = userService.findByUsername(principal.getName());
        PrimaryAccount primaryAccount = user.getPrimaryAccount();
        SavingsAccount savingsAccount = user.getSavingsAccount();

        if (transferFrom.equalsIgnoreCase("Primary") && transferTo.equalsIgnoreCase("Savings")) {
            requireSufficientFunds(primaryAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", amount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (transferFrom.equalsIgnoreCase("Savings") && transferTo.equalsIgnoreCase("Primary")) {
            requireSufficientFunds(savingsAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", amount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransactionException("Invalid Transfer");
        }
    }
    
    public List<Recipient> findRecipientList(Principal principal) {
        String username = principal.getName();
        List<Recipient> recipientList = recipientDao.findAll().stream() 			//convert list to stream
                .filter(recipient -> username.equals(recipient.getUser().getUsername()))	//filters the line, equals to username
                .collect(Collectors.toList());

        return recipientList;
    }

    public Recipient saveRecipient(Recipient recipient) {
        return recipientDao.save(recipient);
    }

    public Recipient findRecipientByName(String recipientName) {
        return recipientDao.findByName(recipientName);
    }

    public void deleteRecipientByName(String recipientName) {
        recipientDao.deleteByName(recipientName);
    }
    
    @Transactional(rollbackFor = InvalidTransactionException.class)
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, BigDecimal amount, Principal principal) throws InvalidTransactionException {
        requirePositive(amount);
        if (recipient == null || recipient.getUser() == null) {
            throw new InvalidTransactionException("Please select a valid recipient.");
        }

        User user = userService.findByUsername(principal.getName());
        if (!user.getUsername().equals(recipient.getUser().getUsername())) {
            throw new InvalidTransactionException("Please select a valid recipient.");
        }

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = user.getPrimaryAccount();
            requireSufficientFunds(primaryAccount.getAccountBalance(), amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = user.getSavingsAccount();
            requireSufficientFunds(savingsAccount.getAccountBalance(), amount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransactionException("Please select a valid account.");
        }
    }

    private void requirePositive(BigDecimal amount) throws InvalidTransactionException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("The amount must be greater than zero.");
        }
    }

    private void requireSufficientFunds(BigDecimal balance, BigDecimal amount) throws InvalidTransactionException {
        if (balance.compareTo(amount) < 0) {
            throw new InvalidTransactionException("Insufficient funds: the available balance does not cover this amount.");
        }
    }
}
