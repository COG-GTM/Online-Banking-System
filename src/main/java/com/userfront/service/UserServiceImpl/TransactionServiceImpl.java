package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
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
import com.userfront.service.InvalidTransactionException;
import com.userfront.service.MoneyAmount;
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


	@Transactional(readOnly = true)
	public List<PrimaryTransaction> findPrimaryTransactionList(String username){
        User user = userService.findByUsername(username);

        return user.getPrimaryAccount().getPrimaryTransactionList();
    }

    @Transactional(readOnly = true)
    public List<SavingsTransaction> findSavingsTransactionList(String username) {
        User user = userService.findByUsername(username);

        return user.getSavingsAccount().getSavingsTransactionList();
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
    public void betweenAccountsTransfer(String transferFrom, String transferTo, BigDecimal amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if (transferFrom.equalsIgnoreCase("Primary") && transferTo.equalsIgnoreCase("Savings")) {
            MoneyAmount.requireSufficientFunds(primaryAccount.getAccountBalance(), amount);

            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Account", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (transferFrom.equalsIgnoreCase("Savings") && transferTo.equalsIgnoreCase("Primary")) {
            MoneyAmount.requireSufficientFunds(savingsAccount.getAccountBalance(), amount);

            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(amount));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Between account transfer from "+transferFrom+" to "+transferTo, "Transfer", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransactionException("Invalid Transfer");
        }
    }

    @Transactional(readOnly = true)
    public List<Recipient> findRecipientList(String username) {
        return recipientDao.findByUserUsername(username);
    }

    public Recipient saveRecipient(Recipient recipient) {
        return recipientDao.save(recipient);
    }

    @Transactional(readOnly = true)
    public Recipient findRecipientByName(String recipientName, String username) {
        return recipientDao.findByNameAndUserUsername(recipientName, username);
    }

    @Transactional(readOnly = true)
    public Recipient findRecipientById(Long id, String username) {
        return recipientDao.findByIdAndUserUsername(id, username);
    }

    @Transactional
    public void deleteRecipientByName(String recipientName, String username) {
        recipientDao.deleteByNameAndUserUsername(recipientName, username);
    }

    @Transactional
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, BigDecimal amount, PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        if (accountType.equalsIgnoreCase("Primary")) {
            MoneyAmount.requireSufficientFunds(primaryAccount.getAccountBalance(), amount);

            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(amount));
            primaryAccountDao.save(primaryAccount);

            PrimaryTransaction primaryTransaction = new PrimaryTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount, primaryAccount.getAccountBalance(), primaryAccount);
            primaryTransactionDao.save(primaryTransaction);
        } else if (accountType.equalsIgnoreCase("Savings")) {
            MoneyAmount.requireSufficientFunds(savingsAccount.getAccountBalance(), amount);

            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(amount));
            savingsAccountDao.save(savingsAccount);

            SavingsTransaction savingsTransaction = new SavingsTransaction(new Date(), "Transfer to recipient "+recipient.getName(), "Transfer", "Finished", amount, savingsAccount.getAccountBalance(), savingsAccount);
            savingsTransactionDao.save(savingsTransaction);
        } else {
            throw new InvalidTransactionException("Invalid account type");
        }
    }
}
