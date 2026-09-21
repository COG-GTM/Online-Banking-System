package com.userfront.service;

import java.math.BigDecimal;
import java.security.Principal;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.util.InvalidAmountException;

public interface AccountService {
	PrimaryAccount createPrimaryAccount();
    SavingsAccount createSavingsAccount();
    void deposit(String accountType, BigDecimal amount, Principal principal) throws InvalidAmountException;
    void withdraw(String accountType, BigDecimal amount, Principal principal) throws InvalidAmountException;
    
    
}
