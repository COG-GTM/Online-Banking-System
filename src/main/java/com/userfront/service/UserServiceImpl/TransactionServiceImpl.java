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
import com.userfront.exception.InsufficientFundsException;
import com.userfront.exception.InvalidAmountException;
import com.userfront.exception.ResourceNotFoundException;
import com.userfront.service.Amounts;
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

    @Override
    public List<PrimaryTransaction> findPrimaryTransactionList(String username) {
        return currentUser(username).getPrimaryAccount().getPrimaryTransactionList();
    }

    @Override
    public List<SavingsTransaction> findSavingsTransactionList(String username) {
        return currentUser(username).getSavingsAccount().getSavingsTransactionList();
    }

    @Override
    public void savePrimaryDepositTransaction(PrimaryTransaction primaryTransaction) {
        primaryTransactionDao.save(primaryTransaction);
    }

    @Override
    public void saveSavingsDepositTransaction(SavingsTransaction savingsTransaction) {
        savingsTransactionDao.save(savingsTransaction);
    }

    @Override
    public void savePrimaryWithdrawTransaction(PrimaryTransaction primaryTransaction) {
        primaryTransactionDao.save(primaryTransaction);
    }

    @Override
    public void saveSavingsWithdrawTransaction(SavingsTransaction savingsTransaction) {
        savingsTransactionDao.save(savingsTransaction);
    }

    @Override
    @Transactional
    public void betweenAccountsTransfer(String transferFrom, String transferTo, BigDecimal amount, Principal principal) {
        BigDecimal value = requirePositive(amount);
        User user = currentUser(principal.getName());
        PrimaryAccount primaryAccount = lockPrimary(user);
        SavingsAccount savingsAccount = lockSavings(user);
        Date date = new Date();

        if ("Primary".equalsIgnoreCase(transferFrom) && "Savings".equalsIgnoreCase(transferTo)) {
            requireSufficientFunds(primaryAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(value));
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().add(value));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            primaryTransactionDao.save(new PrimaryTransaction(date,
                    "Between account transfer from " + transferFrom + " to " + transferTo,
                    "Account", "Finished", value, primaryAccount.getAccountBalance(), primaryAccount));
        } else if ("Savings".equalsIgnoreCase(transferFrom) && "Primary".equalsIgnoreCase(transferTo)) {
            requireSufficientFunds(savingsAccount.getAccountBalance(), value);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(value));
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().add(value));
            primaryAccountDao.save(primaryAccount);
            savingsAccountDao.save(savingsAccount);

            savingsTransactionDao.save(new SavingsTransaction(date,
                    "Between account transfer from " + transferFrom + " to " + transferTo,
                    "Transfer", "Finished", value, savingsAccount.getAccountBalance(), savingsAccount));
        } else {
            throw new InvalidAmountException("Invalid transfer between accounts");
        }
    }

    @Override
    public List<Recipient> findRecipientList(Principal principal) {
        return recipientDao.findByUserUsername(principal.getName());
    }

    @Override
    public Recipient saveRecipient(Recipient recipient) {
        return recipientDao.save(recipient);
    }

    @Override
    public Recipient findRecipientByName(String recipientName, Principal principal) {
        Recipient recipient = recipientDao.findByNameAndUserUsername(recipientName, principal.getName());
        if (recipient == null) {
            throw new ResourceNotFoundException("Recipient not found");
        }
        return recipient;
    }

    @Override
    @Transactional
    public void deleteRecipientByName(String recipientName, Principal principal) {
        if (recipientDao.deleteByNameAndUserUsername(recipientName, principal.getName()) == 0) {
            throw new ResourceNotFoundException("Recipient not found");
        }
    }

    @Override
    @Transactional
    public void toSomeoneElseTransfer(String recipientName, String accountType, BigDecimal amount, Principal principal) {
        BigDecimal value = requirePositive(amount);
        Recipient recipient = findRecipientByName(recipientName, principal);
        User user = currentUser(principal.getName());
        Date date = new Date();

        if ("Primary".equalsIgnoreCase(accountType)) {
            PrimaryAccount primaryAccount = lockPrimary(user);
            requireSufficientFunds(primaryAccount.getAccountBalance(), value);
            primaryAccount.setAccountBalance(primaryAccount.getAccountBalance().subtract(value));
            primaryAccountDao.save(primaryAccount);

            primaryTransactionDao.save(new PrimaryTransaction(date,
                    "Transfer to recipient " + recipient.getName(), "Transfer", "Finished", value,
                    primaryAccount.getAccountBalance(), primaryAccount));
        } else if ("Savings".equalsIgnoreCase(accountType)) {
            SavingsAccount savingsAccount = lockSavings(user);
            requireSufficientFunds(savingsAccount.getAccountBalance(), value);
            savingsAccount.setAccountBalance(savingsAccount.getAccountBalance().subtract(value));
            savingsAccountDao.save(savingsAccount);

            savingsTransactionDao.save(new SavingsTransaction(date,
                    "Transfer to recipient " + recipient.getName(), "Transfer", "Finished", value,
                    savingsAccount.getAccountBalance(), savingsAccount));
        } else {
            throw new InvalidAmountException("Unknown account type: " + accountType);
        }
    }

    private User currentUser(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        return user;
    }

    private PrimaryAccount lockPrimary(User user) {
        return primaryAccountDao.findWithLockById(user.getPrimaryAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Primary account not found"));
    }

    private SavingsAccount lockSavings(User user) {
        return savingsAccountDao.findWithLockById(user.getSavingsAccount().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));
    }

    private static BigDecimal requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        return amount.setScale(Amounts.SCALE, java.math.RoundingMode.UNNECESSARY);
    }

    private static void requireSufficientFunds(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient available balance for this transaction");
        }
    }
}
