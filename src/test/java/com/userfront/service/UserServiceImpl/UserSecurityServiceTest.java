package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.Cookie;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.test.util.ReflectionTestUtils;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;

public class UserSecurityServiceTest {

    private static final String USERNAME = "alice";
    private static final String PASSWORD = "Passw0rd!";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserDao userDao;
    private UserSecurityService userSecurityService;
    private DaoAuthenticationProvider authenticationProvider;
    private User user;

    @Before
    public void setUp() {
        user = new User();
        user.setUsername(USERNAME);
        user.setPassword(passwordEncoder.encode(PASSWORD));

        userDao = mock(UserDao.class);
        when(userDao.findByUsername(USERNAME)).thenReturn(user);

        userSecurityService = new UserSecurityService();
        ReflectionTestUtils.setField(userSecurityService, "userDao", userDao);

        authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userSecurityService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
    }

    @Test
    public void loadedUserDetailsReflectEnabledFlag() {
        UserDetails enabled = userSecurityService.loadUserByUsername(USERNAME);
        assertSame(user, enabled);
        assertTrue(enabled.isEnabled());

        user.setEnabled(false);
        assertFalse(userSecurityService.loadUserByUsername(USERNAME).isEnabled());
    }

    @Test(expected = UsernameNotFoundException.class)
    public void unknownUsernameIsRejected() {
        userSecurityService.loadUserByUsername("nobody");
    }

    @Test
    public void enabledUserAuthenticates() {
        Authentication result = authenticationProvider.authenticate(
                new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD));
        assertTrue(result.isAuthenticated());
        assertEquals(USERNAME, result.getName());
    }

    @Test(expected = DisabledException.class)
    public void disabledUserCannotAuthenticateWithCorrectPassword() {
        user.setEnabled(false);
        authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(USERNAME, PASSWORD));
    }

    @Test
    public void disabledUserCannotAuthenticateWithRememberMeCookie() {
        TokenBasedRememberMeServices rememberMe = new TokenBasedRememberMeServices("test-key", userSecurityService);
        Cookie cookie = issueRememberMeCookie(rememberMe);

        assertNotNull(rememberMe.autoLogin(requestWith(cookie), new MockHttpServletResponse()));

        user.setEnabled(false);
        assertNull(rememberMe.autoLogin(requestWith(cookie), new MockHttpServletResponse()));
    }

    private Cookie issueRememberMeCookie(TokenBasedRememberMeServices rememberMe) {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest();
        loginRequest.setParameter(AbstractRememberMeServices.DEFAULT_PARAMETER, "true");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        rememberMe.loginSuccess(loginRequest, loginResponse,
                new UsernamePasswordAuthenticationToken(USERNAME, null, user.getAuthorities()));
        Cookie cookie = loginResponse.getCookie(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
        assertNotNull(cookie);
        return cookie;
    }

    private static MockHttpServletRequest requestWith(Cookie cookie) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);
        return request;
    }
}
