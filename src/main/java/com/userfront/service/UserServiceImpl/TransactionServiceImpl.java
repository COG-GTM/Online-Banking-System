package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.transfer.InsufficientFundsException;
import com.userfront.service.transfer.InvalidTransferException;
import com.userfront.service.transfer.TransferAmount;

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
    public void betweenAccountsTransfer(String transferFrom, String transferTo, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        boolean primaryToSavings = "Primary".equalsIgnoreCase(transferFrom) && "Savings".equalsIgnoreCase(transferTo);
        boolean savingsToPrimary = "Savings".equalsIgnoreCase(transferFrom) && "Primary".equalsIgnoreCase(transferTo);
        if (!primaryToSavings && !savingsToPrimary) {
            throw new InvalidTransferException("Invalid Transfer");
        }
        BigDecimal value = TransferAmount.parse(amount);
        Date date = new Date();
        String description = "Between account transfer from "+transferFrom+" to "+transferTo;

        if (primaryToSavings) {
            requireDebited(primaryAccountDao.debitIfCovered(primaryAccount.getId(), value));
            savingsAccountDao.credit(savingsAccount.getId(), value);
            BigDecimal primaryBalance = primaryAccountDao.findBalanceById(primaryAccount.getId());

            primaryTransactionDao.save(new PrimaryTransaction(date, description, "Account", "Finished", value.doubleValue(), primaryBalance, primaryAccount));
        } else {
            requireDebited(savingsAccountDao.debitIfCovered(savingsAccount.getId(), value));
            primaryAccountDao.credit(primaryAccount.getId(), value);
            BigDecimal savingsBalance = savingsAccountDao.findBalanceById(savingsAccount.getId());

            savingsTransactionDao.save(new SavingsTransaction(date, description, "Transfer", "Finished", value.doubleValue(), savingsBalance, savingsAccount));
        }
    }
    
    public List<Recipient> findRecipientList(Principal principal) {
        String username = principal.getName();
        List<Recipient> recipientList = recipientDao.findAll().stream() 			//convert list to stream
                .filter(recipient -> username.equals(recipient.getUser().getUsername()))	//filters the line, equals to username
                .collect(Collectors.toList());

        return recipientList;
    }

    public Recipient saveRecipient(Recipient recipient, User user) {
        if (recipient.getId() != null
                && recipientDao.findByIdAndUserUsername(recipient.getId(), user.getUsername()) == null) {
            throw new AccessDeniedException("Recipient does not belong to the current user.");
        }
        recipient.setUser(user);
        return recipientDao.save(recipient);
    }

    public Recipient findRecipientByName(String recipientName, String username) {
        List<Recipient> matches = recipientDao.findByNameAndUserUsername(recipientName, username);
        if (matches.size() > 1) {
            throw new InvalidTransferException("More than one payee is named \"" + recipientName + "\"; rename one before transferring.");
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    @Transactional
    public void deleteRecipientByName(String recipientName, String username) {
        recipientDao.deleteByNameAndUserUsername(recipientName, username);
    }
    
    @Transactional
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if (!isOwnedBy(recipient, primaryAccount)) {
            throw new InvalidTransferException("Unknown recipient.");
        }
        boolean fromPrimary = "Primary".equalsIgnoreCase(accountType);
        if (!fromPrimary && !"Savings".equalsIgnoreCase(accountType)) {
            throw new InvalidTransferException("Invalid account type.");
        }
        BigDecimal value = TransferAmount.parse(amount);
        Date date = new Date();
        String description = "Transfer to recipient "+recipient.getName();

        if (fromPrimary) {
            requireDebited(primaryAccountDao.debitIfCovered(primaryAccount.getId(), value));
            BigDecimal primaryBalance = primaryAccountDao.findBalanceById(primaryAccount.getId());

            primaryTransactionDao.save(new PrimaryTransaction(date, description, "Transfer", "Finished", value.doubleValue(), primaryBalance, primaryAccount));
        } else {
            requireDebited(savingsAccountDao.debitIfCovered(savingsAccount.getId(), value));
            BigDecimal savingsBalance = savingsAccountDao.findBalanceById(savingsAccount.getId());

            savingsTransactionDao.save(new SavingsTransaction(date, description, "Transfer", "Finished", value.doubleValue(), savingsBalance, savingsAccount));
        }
    }

    private static void requireDebited(int updatedRows) {
        if (updatedRows != 1) {
            throw new InsufficientFundsException("Insufficient funds for this transfer.");
        }
    }

    private static boolean isOwnedBy(Recipient recipient, PrimaryAccount primaryAccount) {
        if (recipient == null || recipient.getUser() == null || primaryAccount == null) {
            return false;
        }
        PrimaryAccount ownerAccount = recipient.getUser().getPrimaryAccount();
        return ownerAccount != null && ownerAccount.getId() != null && ownerAccount.getId().equals(primaryAccount.getId());
    }
}
