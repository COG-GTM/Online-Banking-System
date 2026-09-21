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
    public void betweenAccountsTransfer(String transferFrom, String transferTo, BigDecimal amount, User user) throws Exception {
        if (transferFrom.equalsIgnoreCase("Primary") && transferTo.equalsIgnoreCase("Savings")) {
            PrimaryAccount primaryAccount = lockPrimaryAccount(user);
            SavingsAccount savingsAccount = lockSavingsAccount(user);

            debit(primaryAccount, amount);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (transferFrom.equalsIgnoreCase("Savings") && transferTo.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = lockPrimaryAccount(user);
            SavingsAccount savingsAccount = lockSavingsAccount(user);

            debit(savingsAccount, amount);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new Exception("Invalid Transfer");
        }
    }
    
    public List<Recipient> findRecipientList(Principal principal) {
        User user = userService.findByUsername(principal.getName());

        return recipientDao.findByUser(user);
    }

    public Recipient saveRecipient(Recipient recipient) {
        return recipientDao.save(recipient);
    }

    public Recipient findRecipientForUser(Long recipientId, User user) {
        return recipientDao.findByIdAndUser(recipientId, user)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));
    }

    @Transactional
    public void deleteRecipientForUser(Long recipientId, User user) {
        if (recipientDao.deleteByIdAndUser(recipientId, user) == 0) {
            throw new IllegalArgumentException("Recipient not found");
        }
    }

    @Transactional
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, BigDecimal amount, User user) {
        if (!user.getUserId().equals(recipient.getUser().getUserId())) {
            throw new IllegalArgumentException("Recipient not found");
        }

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount primaryAccount = lockPrimaryAccount(user);
            debit(primaryAccount, amount);
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount savingsAccount = lockSavingsAccount(user);
            debit(savingsAccount, amount);
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

    private PrimaryAccount lockPrimaryAccount(User user) {
        return primaryAccountDao.findForUpdate(user.getPrimaryAccount().getId())
                .orElseThrow(() -> new IllegalStateException("Primary account not found"));
    }

    private SavingsAccount lockSavingsAccount(User user) {
        return savingsAccountDao.findForUpdate(user.getSavingsAccount().getId())
                .orElseThrow(() -> new IllegalStateException("Savings account not found"));
    }

    private void debit(PrimaryAccount account, BigDecimal amount) {
        if (account.getAccountBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in primary account");
        }
        account.setAccountBalance(account.getAccountBalance().subtract(amount));
    }

    private void debit(SavingsAccount account, BigDecimal amount) {
        if (account.getAccountBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in savings account");
        }
        account.setAccountBalance(account.getAccountBalance().subtract(amount));
    }
}
