package com.userfront.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ui.ExtendedModelMap;

import com.userfront.dao.RecipientDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;
import com.userfront.service.UserServiceImpl.TransferRejectedException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransferRecipientOwnershipTest {

    @Mock private TransactionService transactionService;
    @Mock private UserService userService;

    @InjectMocks private TransferController controller;

    private final Principal alice = () -> "alice";
    private User user;

    @Before
    public void setUp() {
        user = new User();
        user.setUsername("alice");
        user.setPrimaryAccount(new PrimaryAccount());
        user.setSavingsAccount(new SavingsAccount());
        when(userService.findByUsername("alice")).thenReturn(user);
    }

    @Test
    public void toSomeoneElseLooksUpRecipientWithinCallersOwnList() {
        Recipient own = new Recipient();
        when(transactionService.findRecipientByName("bob", alice)).thenReturn(own);

        String view = controller.toSomeoneElsePost("bob", "Primary", "10", alice, new ExtendedModelMap());

        assertEquals("redirect:/userFront", view);
        verify(transactionService, never()).findRecipientByName(anyString());
        verify(transactionService).toSomeoneElseTransfer(own, "Primary", "10", user.getPrimaryAccount(), user.getSavingsAccount());
    }

    @Test
    public void unknownOrForeignRecipientIsRejectedOnTheForm() {
        when(transactionService.findRecipientByName("someone-elses-payee", alice)).thenReturn(null);
        doThrow(new TransferRejectedException("Please choose a recipient."))
                .when(transactionService).toSomeoneElseTransfer(isNull(), anyString(), anyString(), any(), any());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.toSomeoneElsePost("someone-elses-payee", "Primary", "10", alice, model);

        assertEquals("toSomeoneElse", view);
        assertEquals("Please choose a recipient.", model.get("errorMessage"));
        verify(transactionService).findRecipientList(alice);
    }

    @Test
    public void serviceScopesRecipientLookupToPrincipal() throws Exception {
        RecipientDao recipientDao = mock(RecipientDao.class);
        TransactionServiceImpl service = new TransactionServiceImpl();
        Field field = TransactionServiceImpl.class.getDeclaredField("recipientDao");
        field.setAccessible(true);
        field.set(service, recipientDao);
        Recipient own = new Recipient();
        when(recipientDao.findFirstByNameAndUserUsername("bob", "alice")).thenReturn(own);

        assertEquals(own, service.findRecipientByName("bob", alice));
        assertNull(service.findRecipientByName("", alice));
        assertNull(service.findRecipientByName(null, alice));
        verify(recipientDao, never()).findByName(anyString());
    }
}
