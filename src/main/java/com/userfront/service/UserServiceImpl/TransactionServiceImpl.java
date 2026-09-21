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
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.util.MoneyUtil;

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
        BigDecimal transferAmount = MoneyUtil.parsePositiveAmount(amount);

        if (transferFrom.equalsIgnoreCase("Primary") && transferTo.equalsIgnoreCase("Savings")) {
            PrimaryAccount source = lockPrimaryAccount(primaryAccount);
            SavingsAccount target = lockSavingsAccount(savingsAccount);
            requireSufficientFunds(source.getAccountBalance(), transferAmount);

            source.setAccountBalance(source.getAccountBalance().subtract(transferAmount));
            target.setAccountBalance(target.getAccountBalance().add(transferAmount));
            primaryAccountDao.save(source);
            savingsAccountDao.save(target);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", transferAmount, source.getAccountBalance(), source);
            primaryTransactionDao.save(primaryTransaction);
        } else if (transferFrom.equalsIgnoreCase("Savings") && transferTo.equalsIgnoreCase("Primary")) {
            SavingsAccount source = lockSavingsAccount(savingsAccount);
            PrimaryAccount target = lockPrimaryAccount(primaryAccount);
            requireSufficientFunds(source.getAccountBalance(), transferAmount);

            source.setAccountBalance(source.getAccountBalance().subtract(transferAmount));
            target.setAccountBalance(target.getAccountBalance().add(transferAmount));
            primaryAccountDao.save(target);
            savingsAccountDao.save(source);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", transferAmount, source.getAccountBalance(), source);
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

    public Recipient findRecipientByNameForUser(String recipientName, String username) {
        return recipientDao.findByNameAndUserUsername(recipientName, username);
    }

    @Transactional
    public void deleteRecipientByNameForUser(String recipientName, String username) {
        recipientDao.deleteByNameAndUserUsername(recipientName, username);
    }
    
    @Transactional
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, String amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        BigDecimal transferAmount = MoneyUtil.parsePositiveAmount(amount);

        if (accountType.equalsIgnoreCase("Primary")) {
            PrimaryAccount source = lockPrimaryAccount(primaryAccount);
            requireSufficientFunds(source.getAccountBalance(), transferAmount);

            source.setAccountBalance(source.getAccountBalance().subtract(transferAmount));
            primaryAccountDao.save(source);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount, source.getAccountBalance(), source);
            primaryTransactionDao.save(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            SavingsAccount source = lockSavingsAccount(savingsAccount);
            requireSufficientFunds(source.getAccountBalance(), transferAmount);

            source.setAccountBalance(source.getAccountBalance().subtract(transferAmount));
            savingsAccountDao.save(source);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", transferAmount, source.getAccountBalance(), source);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new IllegalArgumentException("Invalid account type");
        }
    }

    private PrimaryAccount lockPrimaryAccount(PrimaryAccount primaryAccount) {
        return primaryAccountDao.findLockedById(primaryAccount.getId());
    }

    private SavingsAccount lockSavingsAccount(SavingsAccount savingsAccount) {
        return savingsAccountDao.findLockedById(savingsAccount.getId());
    }

    private void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }
}
