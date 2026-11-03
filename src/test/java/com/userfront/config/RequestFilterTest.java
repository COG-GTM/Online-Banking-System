package com.userfront.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import javax.servlet.FilterChain;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class RequestFilterTest {

    private final RequestFilter filter = new RequestFilter();

    @Test
    public void propagatesDownstreamRuntimeException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/deposit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        NumberFormatException failure = new NumberFormatException("For input string: \"abc\"");
        FilterChain chain = (req, res) -> { throw failure; };

        try {
            filter.doFilter(request, response, chain);
            fail("Expected downstream exception to propagate");
        } catch (NumberFormatException e) {
            assertSame(failure, e);
        }
    }

    @Test
    public void preflightIsAnsweredWithoutInvokingChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/deposit");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> fail("Chain must not be invoked for pre-flight");

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }
}
