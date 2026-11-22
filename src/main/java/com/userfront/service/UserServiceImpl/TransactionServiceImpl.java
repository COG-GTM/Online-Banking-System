package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
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

	private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");
	
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
    public void betweenAccountsTransfer(String transferFrom, String transferTo, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        BigDecimal transferAmount = parseTransferAmount(amount);

        if ("Primary".equalsIgnoreCase(transferFrom) && "Savings".equalsIgnoreCase(transferTo)) {
            PrimaryAccount primary = requireAccount(primaryAccount);
            SavingsAccount savings = requireAccount(savingsAccount);
            requireSufficientFunds(primary.getAccountBalance(), transferAmount);
            primary.setAccountBalance(primary.getAccountBalance().subtract(transferAmount));
            savings.setAccountBalance(savings.getAccountBalance().add(transferAmount));
            primaryAccountDao.save(primary);
            savingsAccountDao.save(savings);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", transferAmount.doubleValue(), primary.getAccountBalance(), primary);
            primaryTransactionDao.save(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(transferFrom) && "Primary".equalsIgnoreCase(transferTo)) {
            PrimaryAccount primary = requireAccount(primaryAccount);
            SavingsAccount savings = requireAccount(savingsAccount);
            requireSufficientFunds(savings.getAccountBalance(), transferAmount);
            primary.setAccountBalance(primary.getAccountBalance().add(transferAmount));
            savings.setAccountBalance(savings.getAccountBalance().subtract(transferAmount));
            primaryAccountDao.save(primary);
            savingsAccountDao.save(savings);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", transferAmount.doubleValue(), savings.getAccountBalance(), savings);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransferException("Invalid transfer: choose two different accounts.");
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
    
    /**
     * Payees are external accounts identified by free-text details, so a payee
     * transfer is an outbound payment: it debits the customer's account only.
     */
    @Transactional
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if (recipient == null) {
            throw new InvalidTransferException("Please choose one of your saved recipients.");
        }
        BigDecimal transferAmount = parseTransferAmount(amount);

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primary = requireAccount(primaryAccount);
            requireSufficientFunds(primary.getAccountBalance(), transferAmount);
            primary.setAccountBalance(primary.getAccountBalance().subtract(transferAmount));
            primaryAccountDao.save(primary);

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount.doubleValue(), primary.getAccountBalance(), primary);
            primaryTransactionDao.save(primaryTransaction);
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savings = requireAccount(savingsAccount);
            requireSufficientFunds(savings.getAccountBalance(), transferAmount);
            savings.setAccountBalance(savings.getAccountBalance().subtract(transferAmount));
            savingsAccountDao.save(savings);

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount.doubleValue(), savings.getAccountBalance(), savings);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransferException("Invalid account type.");
        }
    }

    static BigDecimal parseTransferAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidTransferException("Please enter an amount.");
        }
        String trimmed = amount.trim();
        if (!AMOUNT_PATTERN.matcher(trimmed).matches()) {
            throw new InvalidTransferException("Amount must be a positive number with at most two decimal places.");
        }
        BigDecimal transferAmount = new BigDecimal(trimmed);
        if (transferAmount.signum() <= 0) {
            throw new InvalidTransferException("Amount must be greater than zero.");
        }
        return transferAmount;
    }

    private static void requireSufficientFunds(BigDecimal balance, BigDecimal transferAmount) {
        if (balance == null || balance.compareTo(transferAmount) < 0) {
            throw new InvalidTransferException("Insufficient funds.");
        }
    }

    private static <T> T requireAccount(T account) {
        if (account == null) {
            throw new InvalidTransferException("Account not found.");
        }
        return account;
    }
}
