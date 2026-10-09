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
import com.userfront.service.InvalidTransferException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class TransactionServiceImpl implements TransactionService {

	private static final int MAX_AMOUNT_SCALE = 2;
	private static final int MAX_AMOUNT_INTEGER_DIGITS = 17;
	
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
    
    @Transactional
    public void betweenAccountsTransfer(String transferFrom, String transferTo, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) throws Exception {
        BigDecimal transferAmount = parseTransferAmount(amount);

        if (transferFrom.equalsIgnoreCase("Primary") && transferTo.equalsIgnoreCase("Savings")) {
            debitPrimary(primaryAccount, transferAmount);
            savingsAccountDao.credit(savingsAccount.getId(), transferAmount);
            refreshBalances(primaryAccount, savingsAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", transferAmount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (transferFrom.equalsIgnoreCase("Savings") && transferTo.equalsIgnoreCase("Primary")) {
            debitSavings(savingsAccount, transferAmount);
            primaryAccountDao.credit(primaryAccount.getId(), transferAmount);
            refreshBalances(primaryAccount, savingsAccount);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", transferAmount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransferException("Invalid Transfer");
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
    
    @Transactional
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if (recipient == null) {
            throw new InvalidTransferException("Unknown recipient.");
        }
        BigDecimal transferAmount = parseTransferAmount(amount);

        if (accountType.equalsIgnoreCase("Primary")) {
            debitPrimary(primaryAccount, transferAmount);
            primaryAccount.setAccountBalance(primaryAccountDao.findBalanceById(primaryAccount.getId()));

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            debitSavings(savingsAccount, transferAmount);
            savingsAccount.setAccountBalance(savingsAccountDao.findBalanceById(savingsAccount.getId()));

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransferException("Invalid account type.");
        }
    }

    // Atomic "check balance and debit" in a single UPDATE so concurrent transfers cannot overspend.
    private void debitPrimary(PrimaryAccount account, BigDecimal amount) {
        if (primaryAccountDao.debitIfSufficientFunds(account.getId(), amount) != 1) {
            throw new InvalidTransferException("Insufficient funds.");
        }
    }

    private void debitSavings(SavingsAccount account, BigDecimal amount) {
        if (savingsAccountDao.debitIfSufficientFunds(account.getId(), amount) != 1) {
            throw new InvalidTransferException("Insufficient funds.");
        }
    }

    private void refreshBalances(PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        primaryAccount.setAccountBalance(primaryAccountDao.findBalanceById(primaryAccount.getId()));
        savingsAccount.setAccountBalance(savingsAccountDao.findBalanceById(savingsAccount.getId()));
    }

    static BigDecimal parseTransferAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidTransferException("Transfer amount is required.");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidTransferException("Transfer amount must be a number.");
        }
        if (value.signum() <= 0) {
            throw new InvalidTransferException("Transfer amount must be greater than zero.");
        }
        if (value.scale() > MAX_AMOUNT_SCALE) {
            throw new InvalidTransferException("Transfer amount cannot have more than " + MAX_AMOUNT_SCALE + " decimal places.");
        }
        if (value.precision() - value.scale() > MAX_AMOUNT_INTEGER_DIGITS) {
            throw new InvalidTransferException("Transfer amount is too large.");
        }
        return value;
    }
}
