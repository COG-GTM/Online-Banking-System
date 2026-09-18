package com.userfront.service.UserServiceImpl;

import java.security.SecureRandom;

import javax.persistence.EntityExistsException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.userfront.service.AccountNumberService;

@Service
public class AccountNumberServiceImpl implements AccountNumberService {

    private static final int ACCOUNT_NUMBER_ORIGIN = 100000000;
    private static final int ACCOUNT_NUMBER_BOUND = 1000000000;
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 100;

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private AccountNumberReserver accountNumberReserver;

    public int generateAccountNumber() {
        for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
            int accountNumber = ACCOUNT_NUMBER_ORIGIN
                    + secureRandom.nextInt(ACCOUNT_NUMBER_BOUND - ACCOUNT_NUMBER_ORIGIN);

            try {
                accountNumberReserver.reserve(accountNumber);
                return accountNumber;
            } catch (DataIntegrityViolationException | EntityExistsException e) {
                // The number was claimed by another account; try a different one.
            }
        }

        throw new IllegalStateException("Unable to generate a unique account number");
    }

}
