package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.validation.InvalidAmountException;

public class AccountServiceImplTest {

    private AccountServiceImpl service;
    private PrimaryAccount primary;
    private SavingsAccount savings;
    private final Principal principal = () -> "alice";

    @Before
    public void setUp() {
        primary = new PrimaryAccount();
        primary.setAccountBalance(new BigDecimal("100.00"));
        savings = new SavingsAccount();
        savings.setAccountBalance(new BigDecimal("50.00"));
        User user = new User();
        user.setPrimaryAccount(primary);
        user.setSavingsAccount(savings);

        service = new AccountServiceImpl();
        ReflectionTestUtils.setField(service, "userService", stub(UserService.class, user));
        ReflectionTestUtils.setField(service, "transactionService", stub(TransactionService.class, null));
        ReflectionTestUtils.setField(service, "primaryAccountDao", stub(PrimaryAccountDao.class, null));
        ReflectionTestUtils.setField(service, "savingsAccountDao", stub(SavingsAccountDao.class, null));
    }

    @Test
    public void depositAndWithdrawApplyPositiveAmounts() {
        service.deposit("Primary", new BigDecimal("10.25"), principal);
        service.withdraw("Savings", new BigDecimal("5"), principal);

        assertEquals(new BigDecimal("110.25"), primary.getAccountBalance());
        assertEquals(new BigDecimal("45.00"), savings.getAccountBalance());
    }

    @Test
    public void depositAndWithdrawRejectNegativeAmounts() {
        for (String accountType : new String[] {"Primary", "Savings"}) {
            try {
                service.deposit(accountType, new BigDecimal("-10"), principal);
                fail("negative deposit accepted");
            } catch (InvalidAmountException expected) {
            }
            try {
                service.withdraw(accountType, new BigDecimal("-10"), principal);
                fail("negative withdrawal accepted");
            } catch (InvalidAmountException expected) {
            }
        }
        assertEquals(new BigDecimal("100.00"), primary.getAccountBalance());
        assertEquals(new BigDecimal("50.00"), savings.getAccountBalance());
    }

    private <T> T stub(Class<T> type, User user) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getName().equals("findByUsername")) {
                return user;
            }
            return args != null && args.length > 0 ? args[0] : null;
        }));
    }
}
