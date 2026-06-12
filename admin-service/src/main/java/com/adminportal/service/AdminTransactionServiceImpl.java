package com.adminportal.service;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Service
public class AdminTransactionServiceImpl implements TransactionService {

    @Autowired
    private UserService userService;

    @Override
    public List<PrimaryTransaction> findPrimaryTransactionList(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        if (user.getPrimaryAccount() == null) {
            throw new IllegalArgumentException("Primary account not found for user: " + username);
        }
        return user.getPrimaryAccount().getPrimaryTransactionList();
    }

    @Override
    public List<SavingsTransaction> findSavingsTransactionList(String username) {
        User user = userService.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        if (user.getSavingsAccount() == null) {
            throw new IllegalArgumentException("Savings account not found for user: " + username);
        }
        return user.getSavingsAccount().getSavingsTransactionList();
    }

    @Override
    public void savePrimaryDepositTransaction(PrimaryTransaction primaryTransaction) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void saveSavingsDepositTransaction(SavingsTransaction savingsTransaction) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void savePrimaryWithdrawTransaction(PrimaryTransaction primaryTransaction) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void saveSavingsWithdrawTransaction(SavingsTransaction savingsTransaction) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void betweenAccountsTransfer(String transferFrom, String transferTo, String amount,
            PrimaryAccount primaryAccount, SavingsAccount savingsAccount) throws Exception {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public List<Recipient> findRecipientList(Principal principal) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public Recipient saveRecipient(Recipient recipient) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public Recipient findRecipientByName(String recipientName) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void deleteRecipientByName(String recipientName) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }

    @Override
    public void toSomeoneElseTransfer(Recipient recipient, String accountType, String amount,
            PrimaryAccount primaryAccount, SavingsAccount savingsAccount) {
        throw new UnsupportedOperationException("Not supported by admin-service");
    }
}
