package com.userfront.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.userfront.domain.User;
import com.userfront.service.UserService;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "security.login.max-username-failures=3",
        "security.login.max-ip-failures=5"
})
public class LoginBruteForceProtectionTest {

    private static final String PASSWORD = "correct-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Before
    public void setUp() {
        for (String username : new String[] {"alice", "bob", "carol"}) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(username + "@example.com");
            user.setPassword(PASSWORD);
            userService.createUser(user, new HashSet<>());
        }
    }

    private void login(String username, String password, String ip, String expectedRedirect) throws Exception {
        mockMvc.perform(post("/index").param("username", username).param("password", password)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                }))
                .andExpect(redirectedUrl(expectedRedirect));
    }

    @Test
    public void correctPasswordSucceedsBeforeLockout() throws Exception {
        login("alice", "wrong", "10.0.0.1", "/index?error");
        login("alice", PASSWORD, "10.0.0.1", "/userFront");
    }

    @Test
    public void usernameIsLockedAfterRepeatedFailures() throws Exception {
        login("alice", "wrong1", "10.0.0.1", "/index?error");
        login("alice", "wrong2", "10.0.0.2", "/index?error");
        login("alice", "wrong3", "10.0.0.3", "/index?error");
        login("alice", PASSWORD, "10.0.0.4", "/index?locked");
        login("bob", PASSWORD, "10.0.0.4", "/userFront");
    }

    @Test
    public void ipIsBlockedAfterRepeatedFailuresAcrossUsernames() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("user" + i, "wrong", "10.0.0.9", "/index?error");
        }
        login("carol", PASSWORD, "10.0.0.9", "/index?locked");
        login("carol", PASSWORD, "10.0.0.10", "/userFront");
    }
}
