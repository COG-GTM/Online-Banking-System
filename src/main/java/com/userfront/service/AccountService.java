package com.userfront.service;

import java.math.BigDecimal;
import java.security.Principal;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;

public interface AccountService {
	PrimaryAccount createPrimaryAccount();
    SavingsAccount createSavingsAccount();
    void deposit(String accountType, BigDecimal amount, Principal principal);
    void withdraw(String accountType, BigDecimal amount, Principal principal);
    
    
}
