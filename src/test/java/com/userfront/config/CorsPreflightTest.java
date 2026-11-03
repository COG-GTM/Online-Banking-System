package com.userfront.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.dao.UserDao;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, CorsPreflightTest.TestConfig.class})
public class CorsPreflightTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String OTHER_ORIGIN = "https://evil.example";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void preflightFromAllowedOriginIsAnsweredByCorsFilter() throws Exception {
        mockMvc.perform(options("/api/user/all")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization, content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    public void preflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/user/all")
                .header("Origin", OTHER_ORIGIN)
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Credentials"));
    }

    @Test
    public void preflightForDisallowedMethodIsRejected() throws Exception {
        mockMvc.perform(options("/api/user/all")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "PATCH"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    public void nonCorsOptionsRequestStillRequiresAuthentication() throws Exception {
        mockMvc.perform(options("/api/user/all"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/index"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    public void crossOriginGetStillRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/user/all").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isFound())
                .andExpect(redirectedUrlPattern("**/index"));
    }

    @Test
    public void authenticatedCrossOriginGetFromAllowedOriginGetsCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/user/all").header("Origin", ALLOWED_ORIGIN).with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    public void authenticatedCrossOriginHeadFromAllowedOriginGetsCorsHeaders() throws Exception {
        mockMvc.perform(head("/api/user/all").header("Origin", ALLOWED_ORIGIN).with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    public void authenticatedCrossOriginGetFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(get("/api/user/all").header("Origin", OTHER_ORIGIN).with(user("alice")))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    public void sameOriginRequestsGetNoCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/user/all").with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public UserDao userDao() {
            return Mockito.mock(UserDao.class);
        }

        @Bean
        public UserSecurityService userSecurityService() {
            return Mockito.mock(UserSecurityService.class);
        }

        @Bean
        public PingController pingController() {
            return new PingController();
        }
    }

    @RestController
    static class PingController {

        @RequestMapping(value = "/api/user/all", method = RequestMethod.GET)
        public String all() {
            return "ok";
        }
    }
}
