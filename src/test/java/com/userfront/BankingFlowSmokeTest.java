package com.userfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.dao.RoleDao;
import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.domain.security.Role;

// User.toString() touches a lazy collection, so MockMvc result printing is off
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class BankingFlowSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @BeforeEach
    void seedUserRole() {
        if (roleDao.findByName("ROLE_USER") == null) {
            Role role = new Role();
            role.setRoleId(1);
            role.setName("ROLE_USER");
            roleDao.save(role);
        }
    }

    @Test
    void anonymousUserIsSentToLoginPage() throws Exception {
        mockMvc.perform(get("/userFront"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/index"));
        mockMvc.perform(get("/index")).andExpect(status().isOk()).andExpect(view().name("index"));
    }

    @Test
    void signUpDepositTransferAndHistory() throws Exception {
        mockMvc.perform(post("/signup").with(csrf())
                        .param("firstName", "Sample")
                        .param("lastName", "User")
                        .param("phone", "555-0100")
                        .param("email", "sample.user@example.com")
                        .param("username", "sampleuser")
                        .param("password", "sample-pass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        User created = userDao.findByUsername("sampleuser");
        assertThat(created).isNotNull();
        assertThat(created.getPassword()).isNotEqualTo("sample-pass").startsWith("$2a$12$");

        mockMvc.perform(formLogin("/index").user("sampleuser").password("wrong"))
                .andExpect(redirectedUrl("/index?error"));

        MockHttpSession session = (MockHttpSession) mockMvc
                .perform(formLogin("/index").user("sampleuser").password("sample-pass"))
                .andExpect(redirectedUrl("/userFront"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(get("/userFront").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("userFront"));

        mockMvc.perform(post("/account/deposit").session(session).with(csrf())
                        .param("accountType", "Primary").param("amount", "500"))
                .andExpect(redirectedUrl("/userFront"));

        mockMvc.perform(post("/account/deposit").session(session).with(csrf())
                        .param("accountType", "Savings").param("amount", "50"))
                .andExpect(redirectedUrl("/userFront"));

        mockMvc.perform(post("/transfer/betweenAccounts").session(session).with(csrf())
                        .param("transferFrom", "Primary").param("transferTo", "Savings").param("amount", "200"))
                .andExpect(redirectedUrl("/userFront"));

        mockMvc.perform(get("/account/primaryAccount").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("primaryTransactionList", hasSize(2)));

        mockMvc.perform(get("/account/savingsAccount").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("savingsTransactionList", hasSize(1)));

        User after = userDao.findByUsername("sampleuser");
        assertThat(after.getPrimaryAccount().getAccountBalance()).isEqualByComparingTo(new BigDecimal("300"));
        assertThat(after.getSavingsAccount().getAccountBalance()).isEqualByComparingTo(new BigDecimal("250"));

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/index?logout"));
    }
}
