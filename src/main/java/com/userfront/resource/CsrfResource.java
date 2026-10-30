package com.userfront.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hands the session-bound CSRF token to the admin SPA, which runs on another origin and cannot read
 * the Thymeleaf-rendered forms. Only reachable by an authenticated session from the allowed CORS origin.
 */
@RestController
@RequestMapping("/api")
public class CsrfResource {

    @RequestMapping(value = "/csrf", method = RequestMethod.GET)
    public Map<String, String> csrf(CsrfToken token) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("headerName", token.getHeaderName());
        body.put("parameterName", token.getParameterName());
        body.put("token", token.getToken());
        return body;
    }
}
