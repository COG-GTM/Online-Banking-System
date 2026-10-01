package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RequestFilterTest {

    private final RequestFilter filter = new RequestFilter();

    @Test
    public void downstreamExceptionPropagates() throws Exception {
        ServletException failure = new ServletException("boom");
        FilterChain chain = (req, res) -> { throw failure; };

        try {
            filter.doFilter(new MockHttpServletRequest("GET", "/userFront"), new MockHttpServletResponse(), chain);
            fail("expected downstream exception to propagate");
        } catch (ServletException e) {
            assertSame(failure, e);
        }
    }

    @Test
    public void runtimeExceptionPropagates() throws Exception {
        IllegalStateException failure = new IllegalStateException("boom");
        FilterChain chain = (req, res) -> { throw failure; };

        try {
            filter.doFilter(new MockHttpServletRequest("POST", "/deposit"), new MockHttpServletResponse(), chain);
            fail("expected downstream exception to propagate");
        } catch (IllegalStateException e) {
            assertSame(failure, e);
        }
    }

    @Test
    public void requestIsPassedDownTheChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertSame(request, chain.getRequest());
        assertEquals("http://localhost:4200", response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    public void preflightIsAnsweredWithoutCallingTheChain() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest("OPTIONS", "/api/user/all"), response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(null, chain.getRequest());
    }
}
