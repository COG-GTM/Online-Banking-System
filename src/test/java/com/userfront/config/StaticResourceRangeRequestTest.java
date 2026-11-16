package com.userfront.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

class StaticResourceRangeRequestTest {

    private static final String CSS_PATH = "css/main.css";

    private ResourceHttpRequestHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        handler = new ResourceHttpRequestHandler();
        handler.setLocations(Collections.singletonList(new ClassPathResource("static/")));
        handler.setServletContext(new MockServletContext());
        handler.afterPropertiesSet();
    }

    @Test
    void singleRangeIsServedAsPartialContent() throws Exception {
        MockHttpServletResponse response = get("bytes=0-9");

        assertEquals(206, response.getStatus());
        assertEquals(10, response.getContentAsByteArray().length);
    }

    @Test
    void manyOverlappingRangesAreRejected() throws Exception {
        long length = new ClassPathResource("static/" + CSS_PATH).contentLength();
        StringBuilder ranges = new StringBuilder("bytes=");
        for (int i = 0; i < 500; i++) {
            ranges.append(i == 0 ? "" : ",").append("0-").append(length - 1);
        }

        MockHttpServletResponse response = get(ranges.toString());

        assertEquals(416, response.getStatus());
        assertEquals(0, response.getContentAsByteArray().length);
    }

    private MockHttpServletResponse get(String range) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/" + CSS_PATH);
        request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, CSS_PATH);
        request.addHeader(HttpHeaders.RANGE, range);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handleRequest(request, response);
        return response;
    }
}
