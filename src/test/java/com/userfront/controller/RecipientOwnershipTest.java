package com.userfront.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.userfront.dao.PrimaryTransactionDao;
import com.userfront.dao.RecipientDao;
import com.userfront.dao.SavingsTransactionDao;
import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.TransactionServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(SpringRunner.class)
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
@Import(TransactionServiceImpl.class)
public class RecipientOwnershipTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecipientDao recipientDao;

    @Autowired
    private PrimaryTransactionDao primaryTransactionDao;

    @Autowired
    private SavingsTransactionDao savingsTransactionDao;

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private UserService userService;

    private MockMvc mvc;
    private final Principal alicePrincipal = () -> "alice";
    private User alice;
    private User bob;
    private Recipient aliceShared;
    private Recipient bobShared;
    private Recipient bobOnly;

    @Before
    public void setUp() {
        alice = createUser("alice");
        bob = createUser("bob");
        aliceShared = createRecipient("Shared", alice);
        bobShared = createRecipient("Shared", bob);
        bobOnly = createRecipient("Bob only", bob);
        entityManager.flush();
        when(userService.findByUsername("alice")).thenReturn(alice);
        when(userService.findByUsername("bob")).thenReturn(bob);

        TransferController controller = new TransferController();
        ReflectionTestUtils.setField(controller, "transactionService", transactionService);
        ReflectionTestUtils.setField(controller, "userService", userService);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/templates/", ".html"))
                .build();
    }

    @Test
    public void listsOnlyThePrincipalsRecipients() throws Exception {
        mvc.perform(get("/transfer/recipient").principal(alicePrincipal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recipientList", Arrays.asList(aliceShared)));

        List<Recipient> bobRecipients = transactionService.findRecipientList(() -> "bob");
        assertEquals(2, bobRecipients.size());
        assertTrue(bobRecipients.contains(bobShared));
        assertTrue(bobRecipients.contains(bobOnly));
        assertTrue(transactionService.findRecipientList(() -> "nobody").isEmpty());
    }

    @Test
    public void editLooksUpNamesWithinThePrincipalsRecipients() throws Exception {
        mvc.perform(get("/transfer/recipient/edit").param("recipientName", "Shared")
                .principal(alicePrincipal))
                .andExpect(status().isOk())
                .andExpect(model().attribute("recipient", aliceShared));
        for (String name : Arrays.asList("Bob only", "Missing")) {
            mvc.perform(get("/transfer/recipient/edit").param("recipientName", name)
                    .principal(alicePrincipal)).andExpect(status().isNotFound());
        }
    }

    @Test
    public void cannotTransferToAnotherUsersOrMissingRecipient() throws Exception {
        for (String accountType : Arrays.asList("Primary", "Savings")) {
            for (String name : Arrays.asList("Bob only", "Missing")) {
                mvc.perform(post("/transfer/toSomeoneElse").param("recipientName", name)
                        .param("accountType", accountType).param("amount", "10.00")
                        .principal(alicePrincipal)).andExpect(status().isNotFound());
            }
        }
        assertEquals(new BigDecimal("100.00"), alice.getPrimaryAccount().getAccountBalance());
        assertEquals(new BigDecimal("100.00"), alice.getSavingsAccount().getAccountBalance());
        assertEquals(0, primaryTransactionDao.count());
        assertEquals(0, savingsTransactionDao.count());
    }

    @Test
    public void canTransferToAnOwnedRecipientDespiteAnotherUserHavingTheSameName() throws Exception {
        mvc.perform(post("/transfer/toSomeoneElse").param("recipientName", "Shared")
                .param("accountType", "Primary").param("amount", "10.00")
                .principal(alicePrincipal))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/userFront"));
        assertEquals(new BigDecimal("90.00"), alice.getPrimaryAccount().getAccountBalance());
        assertEquals(new BigDecimal("100.00"), bob.getPrimaryAccount().getAccountBalance());
        assertEquals(1, primaryTransactionDao.count());
    }

    @Test
    public void deleteOnlyRemovesTheSelectedOwnedRecipientDespiteDuplicateNames() throws Exception {
        Recipient duplicate = createRecipient("Shared", alice);
        entityManager.flush();
        mvc.perform(post("/transfer/recipient/delete").param("recipientId", aliceShared.getId().toString())
                .principal(alicePrincipal))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/transfer/recipient"));
        entityManager.flush();
        assertFalse(recipientDao.existsById(aliceShared.getId()));
        assertTrue(recipientDao.existsById(duplicate.getId()));
        assertTrue(recipientDao.existsById(bobShared.getId()));
        assertTrue(recipientDao.existsById(bobOnly.getId()));
    }

    @Test
    public void deleteReturns404ForAnotherUsersOrMissingRecipient() throws Exception {
        for (Long id : Arrays.asList(bobOnly.getId(), Long.MAX_VALUE)) {
            mvc.perform(post("/transfer/recipient/delete").param("recipientId", id.toString())
                    .principal(alicePrincipal)).andExpect(status().isNotFound());
        }
        assertEquals(3, recipientDao.count());
        assertTrue(recipientDao.existsById(bobOnly.getId()));
    }

    @Test
    public void getCannotDeleteARecipient() throws Exception {
        mvc.perform(get("/transfer/recipient/delete").param("recipientId", aliceShared.getId().toString())
                .principal(alicePrincipal)).andExpect(status().isMethodNotAllowed());
        assertEquals(3, recipientDao.count());
    }

    @Test
    public void saveRejectsAnotherUsersOrMissingRecipientId() throws Exception {
        for (Long id : Arrays.asList(bobOnly.getId(), Long.MAX_VALUE)) {
            mvc.perform(post("/transfer/recipient/save").param("id", id.toString())
                    .param("name", "Changed").principal(alicePrincipal))
                    .andExpect(status().isNotFound());
        }
        entityManager.flush();
        entityManager.clear();
        Recipient unchanged = recipientDao.findByIdAndUserUsername(bobOnly.getId(), "bob");
        assertEquals("Bob only", unchanged.getName());
        assertEquals(3, recipientDao.count());
    }

    @Test
    public void saveCreatesAndUpdatesOnlyThePrincipalsRecipients() throws Exception {
        mvc.perform(post("/transfer/recipient/save").param("name", "New")
                .param("user.userId", bob.getUserId().toString())
                .principal(alicePrincipal)).andExpect(status().is3xxRedirection());
        Recipient created = recipientDao.findByNameAndUserUsername("New", "alice");
        assertEquals(alice.getUserId(), created.getUser().getUserId());

        mvc.perform(post("/transfer/recipient/save").param("id", aliceShared.getId().toString())
                .param("name", "Updated").param("user.userId", bob.getUserId().toString())
                .principal(alicePrincipal)).andExpect(status().is3xxRedirection());
        entityManager.flush();
        entityManager.clear();
        Recipient updated = recipientDao.findByIdAndUserUsername(aliceShared.getId(), "alice");
        assertEquals("Updated", updated.getName());
        assertTrue(recipientDao.existsById(bobShared.getId()));
    }

    private User createUser(String username) {
        PrimaryAccount primaryAccount = new PrimaryAccount();
        primaryAccount.setAccountBalance(new BigDecimal("100.00"));
        entityManager.persist(primaryAccount);
        SavingsAccount savingsAccount = new SavingsAccount();
        savingsAccount.setAccountBalance(new BigDecimal("100.00"));
        entityManager.persist(savingsAccount);
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.test");
        user.setPrimaryAccount(primaryAccount);
        user.setSavingsAccount(savingsAccount);
        entityManager.persist(user);
        return user;
    }

    private Recipient createRecipient(String name, User user) {
        Recipient recipient = new Recipient();
        recipient.setName(name);
        recipient.setUser(user);
        entityManager.persist(recipient);
        return recipient;
    }
}
