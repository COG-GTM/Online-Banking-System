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
import com.userfront.service.InsufficientFundsException;
import com.userfront.service.TransactionAmount;
import com.userfront.service.TransactionDeclinedException;
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
    
    @Transactional
    public void betweenAccountsTransfer(String transferFrom, String transferTo, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) throws Exception {
        BigDecimal value = TransactionAmount.parse(amount);

        if ("Primary".equalsIgnoreCase(transferFrom) && "Savings".equalsIgnoreCase(transferTo)) {
            if (primaryAccountDao.debitIfSufficient(primaryAccount.getId(), value) != 1) {
                throw new InsufficientFundsException();
            }
            requireUpdated(savingsAccountDao.credit(savingsAccount.getId(), value));
            PrimaryAccount source = primaryAccountDao.findById(primaryAccount.getId()).get();

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", value.doubleValue(), source.getAccountBalance(), source);
            primaryTransactionDao.save(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(transferFrom) && "Primary".equalsIgnoreCase(transferTo)) {
            // Lock primary before savings in both directions so opposite transfers cannot deadlock.
            primaryAccountDao.lockById(primaryAccount.getId());
            if (savingsAccountDao.debitIfSufficient(savingsAccount.getId(), value) != 1) {
                throw new InsufficientFundsException();
            }
            requireUpdated(primaryAccountDao.credit(primaryAccount.getId(), value));
            SavingsAccount source = savingsAccountDao.findById(savingsAccount.getId()).get();

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", value.doubleValue(), source.getAccountBalance(), source);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new TransactionDeclinedException("Select two different accounts to transfer between.");
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
            throw new TransactionDeclinedException("Select a recipient.");
        }
        BigDecimal value = TransactionAmount.parse(amount);

        if ("Primary".equalsIgnoreCase(accountType)) {
            if (primaryAccountDao.debitIfSufficient(primaryAccount.getId(), value) != 1) {
                throw new InsufficientFundsException();
            }
            PrimaryAccount source = primaryAccountDao.findById(primaryAccount.getId()).get();

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", value.doubleValue(), source.getAccountBalance(), source);
            primaryTransactionDao.save(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            if (savingsAccountDao.debitIfSufficient(savingsAccount.getId(), value) != 1) {
                throw new InsufficientFundsException();
            }
            SavingsAccount source = savingsAccountDao.findById(savingsAccount.getId()).get();

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", value.doubleValue(), source.getAccountBalance(), source);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new TransactionDeclinedException("Select an account.");
        }
    }

    private static void requireUpdated(int rows) {
        if (rows != 1) {
            throw new IllegalStateException("Account not found");
        }
    }
}
