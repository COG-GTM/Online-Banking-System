package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
import org.springframework.dao.DataAccessResourceFailureException;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceImplTest {

    @Mock
    private AccountNumberAllocator accountNumberAllocator;

    @Mock
    private PrimaryAccountDao primaryAccountDao;

    @Mock
    private SavingsAccountDao savingsAccountDao;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    public void accountNumbersAreNineDigitsUniqueAndNotSequential() {
        when(accountNumberAllocator.reserve(anyInt())).thenReturn(true);
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
    public void numberAlreadyTakenIsSkipped() {
        when(accountNumberAllocator.reserve(anyInt())).thenReturn(false, false, true);

        accountService.createSavingsAccount();

        ArgumentCaptor<Integer> tried = ArgumentCaptor.forClass(Integer.class);
        verify(accountNumberAllocator, times(3)).reserve(tried.capture());
        ArgumentCaptor<SavingsAccount> saved = ArgumentCaptor.forClass(SavingsAccount.class);
        verify(savingsAccountDao).save(saved.capture());
        assertEquals(tried.getAllValues().get(2).intValue(), saved.getValue().getAccountNumber());
    }

    @Test
    public void databaseErrorsPropagateInsteadOfBeingRetried() {
        when(accountNumberAllocator.reserve(anyInt()))
                .thenThrow(new DataAccessResourceFailureException("table missing"));
        try {
            accountService.createPrimaryAccount();
            fail("expected the database error to propagate");
        } catch (DataAccessResourceFailureException expected) {
            verify(accountNumberAllocator, times(1)).reserve(anyInt());
            verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void failsRatherThanReusingANumberWhenNoneIsFree() {
        when(accountNumberAllocator.reserve(anyInt())).thenReturn(false);
        try {
            accountService.createPrimaryAccount();
        } finally {
            verify(accountNumberAllocator, times(AccountServiceImpl.ACCOUNT_NUMBER_MAX_ATTEMPTS)).reserve(anyInt());
            verify(primaryAccountDao, never()).save(any(PrimaryAccount.class));
        }
    }
}
