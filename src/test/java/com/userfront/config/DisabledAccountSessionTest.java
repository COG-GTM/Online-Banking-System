package com.userfront.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.userfront.domain.security.Role;
import com.userfront.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
class DisabledAccountSessionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleDao roleDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserService userService;

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
    void disablingUserEndsTheirOpenSession() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("firstName", "Mallory").param("lastName", "User").param("phone", "555-0101")
                        .param("email", "mallory@example.com").param("username", "mallory")
                        .param("password", "mallory-pass"))
                .andExpect(redirectedUrl("/"));

        MockHttpSession session = (MockHttpSession) mockMvc
                .perform(formLogin("/index").user("mallory").password("mallory-pass"))
                .andExpect(redirectedUrl("/userFront"))
                .andReturn().getRequest().getSession();

        mockMvc.perform(post("/account/deposit").session(session)
                        .param("accountType", "Primary").param("amount", "100"))
                .andExpect(redirectedUrl("/userFront"));

        userService.disableUser("mallory");

        mockMvc.perform(post("/account/withdraw").session(session)
                        .param("accountType", "Primary").param("amount", "40"))
                .andExpect(redirectedUrl("/index?disabled"));

        assertThat(session.isInvalid()).isTrue();
        assertThat(userDao.findByUsername("mallory").getPrimaryAccount().getAccountBalance())
                .isEqualByComparingTo(new BigDecimal("100"));

        mockMvc.perform(get("/index").param("disabled", ""))
                .andExpect(status().isOk());
        mockMvc.perform(formLogin("/index").user("mallory").password("mallory-pass"))
                .andExpect(redirectedUrl("/index?error"));
    }
}
