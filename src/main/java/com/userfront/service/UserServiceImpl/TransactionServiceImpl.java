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
import com.userfront.service.TransferAmount;
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
    
    @Transactional
    public void betweenAccountsTransfer(String transferFrom, String transferTo, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) throws Exception {
        boolean primaryToSavings = "Primary".equalsIgnoreCase(transferFrom) && "Savings".equalsIgnoreCase(transferTo);
        boolean savingsToPrimary = "Savings".equalsIgnoreCase(transferFrom) && "Primary".equalsIgnoreCase(transferTo);
        if (!primaryToSavings && !savingsToPrimary) {
            throw new InvalidTransferException("Invalid Transfer");
        }
        BigDecimal value = TransferAmount.parse(amount);

        if (primaryToSavings) {
            TransferAmount.requireCovered(primaryAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(value));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(value));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", value.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else {
            TransferAmount.requireCovered(savingsAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(value));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(value));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", value.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
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
            throw new InvalidTransferException("Please choose one of your saved recipients.");
        }
        boolean fromPrimary = "Primary".equalsIgnoreCase(accountType);
        if (!fromPrimary && !"Savings".equalsIgnoreCase(accountType)) {
            throw new InvalidTransferException("Please select the account to transfer from.");
        }
        BigDecimal value = TransferAmount.parse(amount);

        if (fromPrimary) {
            TransferAmount.requireCovered(primaryAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(value));
            primaryAccountDao.save(primaryAccount);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", value.doubleValue(), primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else {
            TransferAmount.requireCovered(savingsAccount.getAccountBalance(), value);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(value));
            savingsAccountDao.save(savingsAccount);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", value.doubleValue(), savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        }
    }
}
