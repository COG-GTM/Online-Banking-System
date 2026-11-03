package com.userfront.config;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.userfront.controller.HomeController;
import com.userfront.dao.RoleDao;
import com.userfront.domain.User;
import com.userfront.service.LoginAttemptService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = HomeController.class)
@Import({SecurityConfig.class, LoginAttemptService.class})
@TestPropertySource(properties = {
        "security.login.max-failures-per-user=3",
        "security.login.max-failures-per-address=6",
        "security.login.lockout-minutes=15"
})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class LoginThrottlingTest {

    private static final String PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @MockBean
    private UserSecurityService userSecurityService;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleDao roleDao;

    @Before
    public void setUp() {
        User alice = new User();
        alice.setUsername("alice");
        alice.setPassword(passwordEncoder.encode(PASSWORD));
        doThrow(new UsernameNotFoundException("not found")).when(userSecurityService).loadUserByUsername(anyString());
        doReturn(alice).when(userSecurityService).loadUserByUsername("alice");
    }

    @Test
    public void lockedUsernameIsRejectedWithoutCheckingCredentials() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("alice", "wrong", "10.0.0.1")).andExpect(redirectedUrl("/index?error"));
        }

        mockMvc.perform(login("alice", PASSWORD, "10.0.0.2")).andExpect(redirectedUrl("/index?locked"));
        verify(userSecurityService, times(3)).loadUserByUsername("alice");
    }

    @Test
    public void successfulLoginResetsUsernameCounter() throws Exception {
        mockMvc.perform(login("alice", "wrong", "10.0.0.1")).andExpect(redirectedUrl("/index?error"));
        mockMvc.perform(login("alice", "wrong", "10.0.0.1")).andExpect(redirectedUrl("/index?error"));
        mockMvc.perform(login("alice", PASSWORD, "10.0.0.1")).andExpect(redirectedUrl("/userFront"));

        mockMvc.perform(login("alice", "wrong", "10.0.0.1")).andExpect(redirectedUrl("/index?error"));
        mockMvc.perform(login("alice", "wrong", "10.0.0.1")).andExpect(redirectedUrl("/index?error"));
        mockMvc.perform(login("alice", PASSWORD, "10.0.0.1")).andExpect(redirectedUrl("/userFront"));
    }

    @Test
    public void unknownUsernamesLockTheSameWayAsRealOnes() throws Exception {
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(login("ghost", "wrong", "10.0.0.1")).andExpect(redirectedUrl("/index?error"));
        }

        mockMvc.perform(login("ghost", "wrong", "10.0.0.2")).andExpect(redirectedUrl("/index?locked"));
    }

    @Test
    public void addressIsBlockedAfterSprayingManyUsernames() throws Exception {
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(login("user" + i, "wrong", "10.0.0.7")).andExpect(redirectedUrl("/index?error"));
        }

        mockMvc.perform(login("alice", PASSWORD, "10.0.0.7")).andExpect(redirectedUrl("/index?locked"));
        mockMvc.perform(login("alice", PASSWORD, "10.0.0.8")).andExpect(redirectedUrl("/userFront"));
        verify(userSecurityService, never()).loadUserByUsername("user6");
    }

    @Test
    public void loginPageShowsLockoutMessage() throws Exception {
        mockMvc.perform(get("/index").param("locked", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Too many failed sign-in attempts")));
    }

    @Test
    public void loginPageStillShowsInvalidCredentialsMessage() throws Exception {
        mockMvc.perform(get("/index").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username and secret.")));
    }

    private static MockHttpServletRequestBuilder login(String username, String password, String address) {
        return post("/index")
                .param("username", username)
                .param("password", password)
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                });
    }
}
