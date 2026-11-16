package com.userfront.service.UserServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.userfront.dao.UserDao;
import com.userfront.domain.User;
import com.userfront.domain.security.UserRole;
import com.userfront.service.UserService;

@RunWith(MockitoJUnitRunner.class)
public class UsernameLoggingTest {

    private static final String FORGED =
            "nobody\r\n2026-11-16 13:30:00.000  WARN 1 --- [nio-8080-exec-1] c.u.s.U.UserSecurityService : Username admin not found";

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserSecurityService userSecurityService;

    @InjectMocks
    private UserServiceImpl userService;

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Logger securityLogger;
    private Logger userServiceLogger;

    @Before
    public void attachAppender() {
        appender.start();
        securityLogger = (Logger) LoggerFactory.getLogger(UserSecurityService.class);
        userServiceLogger = (Logger) LoggerFactory.getLogger(UserService.class);
        securityLogger.addAppender(appender);
        userServiceLogger.addAppender(appender);
    }

    @After
    public void detachAppender() {
        securityLogger.detachAppender(appender);
        userServiceLogger.detachAppender(appender);
    }

    @Test
    public void unknownLoginUsernameIsLoggedOnOneLine() {
        try {
            userSecurityService.loadUserByUsername(FORGED);
            fail("expected UsernameNotFoundException");
        } catch (UsernameNotFoundException e) {
            assertNoLineBreak(e.getMessage());
        }

        assertEquals(1, appender.list.size());
        String logged = appender.list.get(0).getFormattedMessage();
        assertNoLineBreak(logged);
        assertEquals("Username nobody\\r\\n2026-11-16 13:30:00.000  WARN 1 --- [nio-8080-exec-1] "
                + "c.u.s.U.UserSecurityService : Username admin not found not found", logged);
    }

    @Test
    public void knownLoginUsernameStillResolves() {
        User user = new User();
        user.setUsername("alice");
        when(userDao.findByUsername("alice")).thenReturn(user);

        assertSame(user, userSecurityService.loadUserByUsername("alice"));
        assertEquals(0, appender.list.size());
    }

    @Test
    public void duplicateSignupUsernameIsLoggedOnOneLine() {
        User existing = new User();
        when(userDao.findByUsername(FORGED)).thenReturn(existing);
        User signup = new User();
        signup.setUsername(FORGED);

        assertSame(existing, userService.createUser(signup, Collections.<UserRole>emptySet()));

        assertEquals(1, appender.list.size());
        assertNoLineBreak(appender.list.get(0).getFormattedMessage());
    }

    private static void assertNoLineBreak(String s) {
        assertFalse("contains CR: " + s, s.indexOf('\r') >= 0);
        assertFalse("contains LF: " + s, s.indexOf('\n') >= 0);
    }
}
