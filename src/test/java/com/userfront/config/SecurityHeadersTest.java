package com.userfront.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SecurityHeadersTest.TestConfig.class)
public class SecurityHeadersTest {

    @Configuration
    @EnableWebMvc
    @Import(SecurityConfig.class)
    static class TestConfig {
    }

    @MockBean
    private UserSecurityService userSecurityService;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
    }

    @Test
    public void writesHardeningHeadersOnPublicPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    public void writesHardeningHeadersOnLoginRedirect() throws Exception {
        mockMvc.perform(get("/userFront"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    public void writesHstsOnSecureRequests() throws Exception {
        mockMvc.perform(get("/").secure(true))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=" + SecurityConfig.HSTS_MAX_AGE_SECONDS + " ; includeSubDomains"));
    }

    @Test
    public void omitsHstsOnPlainHttpRequests() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }
}
