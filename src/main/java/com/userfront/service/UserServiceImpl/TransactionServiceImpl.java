package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

	@PersistenceContext
	private EntityManager entityManager;
	

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
        User owner = authenticatedUser();

        if (transferFrom.equalsIgnoreCase("Primary") && transferTo.equalsIgnoreCase("Savings")) {
            PrimaryAccount lockedPrimary = lockPrimaryAccount(owner, primaryAccount);
            SavingsAccount lockedSavings = lockSavingsAccount(owner, savingsAccount);
            requireSufficientFunds(lockedPrimary.getAccountBalance(), transferAmount);
            lockedPrimary.setAccountBalance(lockedPrimary.getAccountBalance().subtract(transferAmount));
            lockedSavings.setAccountBalance(lockedSavings.getAccountBalance().add(transferAmount));
            primaryAccountDao.save(lockedPrimary);
            savingsAccountDao.save(lockedSavings);
            primaryAccount.setAccountBalance(lockedPrimary.getAccountBalance());
            savingsAccount.setAccountBalance(lockedSavings.getAccountBalance());

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", transferAmount.doubleValue(), lockedPrimary.getAccountBalance(), lockedPrimary);
            primaryTransactionDao.save(primaryTransaction);
        } else if (transferFrom.equalsIgnoreCase("Savings") && transferTo.equalsIgnoreCase("Primary")) {
            PrimaryAccount lockedPrimary = lockPrimaryAccount(owner, primaryAccount);
            SavingsAccount lockedSavings = lockSavingsAccount(owner, savingsAccount);
            requireSufficientFunds(lockedSavings.getAccountBalance(), transferAmount);
            lockedPrimary.setAccountBalance(lockedPrimary.getAccountBalance().add(transferAmount));
            lockedSavings.setAccountBalance(lockedSavings.getAccountBalance().subtract(transferAmount));
            primaryAccountDao.save(lockedPrimary);
            savingsAccountDao.save(lockedSavings);
            primaryAccount.setAccountBalance(lockedPrimary.getAccountBalance());
            savingsAccount.setAccountBalance(lockedSavings.getAccountBalance());

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", transferAmount.doubleValue(), lockedSavings.getAccountBalance(), lockedSavings);
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
        BigDecimal transferAmount = parseTransferAmount(amount);
        User owner = authenticatedUser();
        requireOwnedRecipient(recipient, owner);

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount lockedPrimary = lockPrimaryAccount(owner, primaryAccount);
            requireSufficientFunds(lockedPrimary.getAccountBalance(), transferAmount);
            lockedPrimary.setAccountBalance(lockedPrimary.getAccountBalance().subtract(transferAmount));
            primaryAccountDao.save(lockedPrimary);
            primaryAccount.setAccountBalance(lockedPrimary.getAccountBalance());

            Date date = new Date();

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount.doubleValue(), lockedPrimary.getAccountBalance(), lockedPrimary);
            primaryTransactionDao.save(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount lockedSavings = lockSavingsAccount(owner, savingsAccount);
            requireSufficientFunds(lockedSavings.getAccountBalance(), transferAmount);
            lockedSavings.setAccountBalance(lockedSavings.getAccountBalance().subtract(transferAmount));
            savingsAccountDao.save(lockedSavings);
            savingsAccount.setAccountBalance(lockedSavings.getAccountBalance());

            Date date = new Date();

            SavingsTransaction savingsTransaction = new SavingsTransaction(date, "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount.doubleValue(), lockedSavings.getAccountBalance(), lockedSavings);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransferException("Invalid account type");
        }
    }

    private User authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = authentication == null ? null : userService.findByUsername(authentication.getName());
        if (user == null) {
            throw new InvalidTransferException("Not authenticated");
        }
        return user;
    }

    private PrimaryAccount lockPrimaryAccount(User owner, PrimaryAccount requested) {
        PrimaryAccount owned = owner.getPrimaryAccount();
        if (owned == null || owned.getId() == null || requested == null || !owned.getId().equals(requested.getId())) {
            throw new InvalidTransferException("Invalid account");
        }
        PrimaryAccount locked = primaryAccountDao.findByIdForUpdate(owned.getId());
        if (locked == null) {
            throw new InvalidTransferException("Invalid account");
        }
        entityManager.refresh(locked);
        return locked;
    }

    private SavingsAccount lockSavingsAccount(User owner, SavingsAccount requested) {
        SavingsAccount owned = owner.getSavingsAccount();
        if (owned == null || owned.getId() == null || requested == null || !owned.getId().equals(requested.getId())) {
            throw new InvalidTransferException("Invalid account");
        }
        SavingsAccount locked = savingsAccountDao.findByIdForUpdate(owned.getId());
        if (locked == null) {
            throw new InvalidTransferException("Invalid account");
        }
        entityManager.refresh(locked);
        return locked;
    }

    private static BigDecimal parseTransferAmount(String amount) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new InvalidTransferException("Transfer amount is required");
        }
        BigDecimal transferAmount;
        try {
            transferAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException e) {
            throw new InvalidTransferException("Transfer amount must be a number");
        }
        if (transferAmount.signum() <= 0) {
            throw new InvalidTransferException("Transfer amount must be positive");
        }
        if (transferAmount.stripTrailingZeros().scale() > 2) {
            throw new InvalidTransferException("Transfer amount must have at most two decimal places");
        }
        return transferAmount;
    }

    private static void requireSufficientFunds(BigDecimal balance, BigDecimal transferAmount) {
        if (balance == null || balance.compareTo(transferAmount) < 0) {
            throw new InvalidTransferException("Insufficient funds");
        }
    }

    private static void requireOwnedRecipient(Recipient recipient, User owner) {
        if (recipient == null || recipient.getUser() == null || owner.getUsername() == null
                || !owner.getUsername().equals(recipient.getUser().getUsername())) {
            throw new InvalidTransferException("Invalid recipient");
        }
    }
}
