package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceImplTest {

    @Mock
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    public void accountNumbersAreNineDigitsUniqueAndNotSequential() {
        int count = 50;
        for (int i = 0; i < count; i++) {
            accountService.createPrimaryAccount();
        }
        ArgumentCaptor<PrimaryAccount> saved = ArgumentCaptor.forClass(PrimaryAccount.class);
        verify(primaryAccountDao, times(count)).save(saved.capture());

        List<Integer> numbers = new ArrayList<>();
        for (PrimaryAccount account : saved.getAllValues()) {
            numbers.add(account.getAccountNumber());
        }
        assertEquals(count, new HashSet<>(numbers).size());
        for (int i = 0; i < numbers.size(); i++) {
            int n = numbers.get(i);
            assertTrue(n >= AccountServiceImpl.ACCOUNT_NUMBER_MIN && n <= AccountServiceImpl.ACCOUNT_NUMBER_MAX);
            if (i > 0) {
                assertTrue(Math.abs(n - numbers.get(i - 1)) != 1);
            }
        }
    }

    @Test
    public void numberAlreadyUsedInEitherTableIsSkipped() {
        when(primaryAccountDao.findByAccountNumber(anyInt()))
                .thenReturn(new PrimaryAccount())
                .thenReturn(null);
        when(savingsAccountDao.findByAccountNumber(anyInt()))
                .thenReturn(new SavingsAccount())
                .thenReturn(null);

        accountService.createSavingsAccount();

        verify(primaryAccountDao, times(3)).findByAccountNumber(anyInt());
        verify(savingsAccountDao, times(3)).findByAccountNumber(anyInt());
        verify(savingsAccountDao).save(any(SavingsAccount.class));
    }

    @Test(expected = IllegalStateException.class)
    public void failsRatherThanReusingANumberWhenNoneIsFree() {
        when(primaryAccountDao.findByAccountNumber(anyInt())).thenReturn(new PrimaryAccount());
        try {
            accountService.createPrimaryAccount();
        } finally {
            verify(primaryAccountDao, times(AccountServiceImpl.ACCOUNT_NUMBER_MAX_ATTEMPTS)).findByAccountNumber(anyInt());
            verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        }
    }
}
