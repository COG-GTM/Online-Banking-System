package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

public class RequestFilterTest {

    private final RequestFilter filter = new RequestFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private PrintStream originalErr;
    private Level originalLevel;
    private boolean originalAdditive;

    @Before
    public void setUp() {
        originalLevel = logger.getLevel();
        originalAdditive = logger.isAdditive();
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        appender.start();
        logger.addAppender(appender);
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(err, true));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        logger.setAdditive(originalAdditive);
    }

    @Test
    public void preflightIsLoggedThroughSlf4jNotStdout() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/user/all"), response, (req, res) -> {
            throw new AssertionError("pre-flight must not reach the chain");
        });

        assertEquals(200, response.getStatus());
        assertEquals("", out.toString());
        assertEquals("", err.toString());
        assertEquals(1, appender.list.size());
        assertEquals(Level.DEBUG, appender.list.get(0).getLevel());
        assertEquals("Handling CORS pre-flight request", appender.list.get(0).getFormattedMessage());
    }

    @Test
    public void chainFailureIsLoggedThroughSlf4jNotStderr() {
        ServletException failure = new ServletException("boom");
        FilterChain failingChain = (ServletRequest req, ServletResponse res) -> {
            throw failure;
        };

        filter.doFilter(new MockHttpServletRequest("GET", "/userFront"), new MockHttpServletResponse(), failingChain);

        assertEquals("", out.toString());
        assertEquals("", err.toString());
        assertEquals(1, appender.list.size());
        ILoggingEvent event = appender.list.get(0);
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals("Request processing failed: GET /userFront", event.getFormattedMessage());
        assertSame(failure, ((ThrowableProxy) event.getThrowableProxy()).getThrowable());
    }
}
