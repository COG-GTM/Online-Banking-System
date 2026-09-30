package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private List<Integer> createPrimaryAccounts(int count) {
        ArgumentCaptor<PrimaryAccount> saved = ArgumentCaptor.forClass(PrimaryAccount.class);
        for (int i = 0; i < count; i++) {
            accountService.createPrimaryAccount();
        }
        verify(primaryAccountDao, times(count)).save(saved.capture());
        List<Integer> numbers = new ArrayList<>();
        for (PrimaryAccount account : saved.getAllValues()) {
            numbers.add(account.getAccountNumber());
        }
        return numbers;
    }

    @Test
    public void accountNumbersAreNineDigitAndNotSequential() {
        List<Integer> numbers = createPrimaryAccounts(50);

        Set<Integer> unique = new HashSet<>(numbers);
        assertEquals(numbers.size(), unique.size());
        int consecutive = 0;
        for (int i = 0; i < numbers.size(); i++) {
            int n = numbers.get(i);
            assertTrue(n >= AccountServiceImpl.ACCOUNT_NUMBER_MIN && n <= AccountServiceImpl.ACCOUNT_NUMBER_MAX);
            if (i > 0 && n == numbers.get(i - 1) + 1) {
                consecutive++;
            }
        }
        assertEquals(0, consecutive);
    }

    @Test
    public void numberTakenInEitherTableIsSkipped() {
        when(primaryAccountDao.findByAccountNumber(anyInt()))
                .thenReturn(new PrimaryAccount())
                .thenReturn(null);
        when(savingsAccountDao.findByAccountNumber(anyInt()))
                .thenReturn(new SavingsAccount())
                .thenReturn(null);

        ArgumentCaptor<SavingsAccount> saved = ArgumentCaptor.forClass(SavingsAccount.class);
        accountService.createSavingsAccount();

        verify(savingsAccountDao).save(saved.capture());
        verify(primaryAccountDao, atLeast(3)).findByAccountNumber(anyInt());
        assertNotEquals(0, saved.getValue().getAccountNumber());
    }

    @Test(expected = IllegalStateException.class)
    public void failsInsteadOfReusingANumberWhenNoFreeNumberIsFound() {
        when(primaryAccountDao.findByAccountNumber(anyInt())).thenReturn(new PrimaryAccount());
        try {
            accountService.createPrimaryAccount();
        } finally {
            verify(primaryAccountDao, times(AccountServiceImpl.ACCOUNT_NUMBER_MAX_ATTEMPTS)).findByAccountNumber(anyInt());
            verify(primaryAccountDao, times(0)).save(any(PrimaryAccount.class));
        }
    }
}
