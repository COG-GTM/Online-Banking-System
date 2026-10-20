package com.userfront.resource;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CsrfResource {

    @RequestMapping(value = "/csrf", method = RequestMethod.GET)
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}
