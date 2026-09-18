package com.userfront.service.UserServiceImpl;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.userfront.service.AccountNumberService;

@Service
public class AccountNumberServiceImpl implements AccountNumberService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ACCOUNT_NUMBER_ORIGIN = 100000000;
    private static final int ACCOUNT_NUMBER_BOUND = 900000000;
    private static final int MAX_ATTEMPTS = 20;

    @Autowired
    private AccountNumberAllocator accountNumberAllocator;

    public int reserveAccountNumber() {
        DataIntegrityViolationException lastFailure = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            int accountNumber = ACCOUNT_NUMBER_ORIGIN + RANDOM.nextInt(ACCOUNT_NUMBER_BOUND);

            try {
                accountNumberAllocator.allocate(accountNumber);
                return accountNumber;
            } catch (DataIntegrityViolationException e) {
                lastFailure = e;
            }
        }

        throw new IllegalStateException("Unable to reserve a unique account number", lastFailure);
    }
}
