package com.userfront.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.userfront.config.SecurityConfig;
import com.userfront.dao.RoleDao;
import com.userfront.resource.UserResource;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {HomeController.class, UserResource.class})
@Import(SecurityConfig.class)
public class RenderedFormCsrfTest {

    private static final Pattern CSRF_INPUT = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleDao roleDao;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private UserSecurityService userSecurityService;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void tokenRenderedIntoThymeleafFormIsAcceptedOnPost() throws Exception {
        MvcResult page = mockMvc.perform(get("/index")).andExpect(status().isOk()).andReturn();

        Matcher matcher = CSRF_INPUT.matcher(page.getResponse().getContentAsString());
        assertTrue("login form should contain a rendered _csrf hidden input", matcher.find());
        String token = matcher.group(1);
        Cookie xsrfCookie = page.getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(xsrfCookie);

        mockMvc.perform(post("/api/user/bob/disable").cookie(xsrfCookie).param("_csrf", token))
                .andExpect(status().isOk());
        verify(userService).disableUser("bob");
    }
}
