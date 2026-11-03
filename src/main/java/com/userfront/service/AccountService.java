package com.userfront.service;

import java.security.Principal;

import com.userfront.domain.AccountType;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;

public interface AccountService {
	PrimaryAccount createPrimaryAccount();
    SavingsAccount createSavingsAccount();
    void deposit(AccountType accountType, double amount, Principal principal);
    void withdraw(AccountType accountType, double amount, Principal principal);
    
    
}
