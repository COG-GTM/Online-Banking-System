package com.userfront.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.userfront.dao.UserDao;
import com.userfront.service.UserServiceImpl.UserSecurityService;

@RunWith(SpringRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {SecurityConfig.class, SecurityConfigPublicMatchersTest.TestConfig.class})
public class SecurityConfigPublicMatchersTest {

    @Configuration
    @EnableWebMvc
    static class TestConfig {

        @Bean
        public UserDao userDao() {
            return Mockito.mock(UserDao.class);
        }

        @Bean
        public UserSecurityService userSecurityService() {
            return new UserSecurityService();
        }

        @Bean
        public ProbeController probeController() {
            return new ProbeController();
        }
    }

    @RestController
    static class ProbeController {

        @RequestMapping({"/", "/signup", "/error", "/css/site.css", "/js/app.js", "/images/logo.png",
                "/console", "/console/login.do", "/about", "/about/team", "/contact", "/contact/form",
                "/webjars/jquery/jquery.js", "/error/500/detail", "/userFront", "/api/user/all"})
        public String ok() {
            return "ok";
        }
    }

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
    public void intendedPublicPathsAreReachableAnonymously() throws Exception {
        for (String path : new String[] {"/", "/signup", "/error", "/css/site.css", "/js/app.js", "/images/logo.png"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    public void h2ConsolePathRequiresAuthentication() throws Exception {
        assertRedirectsToLogin("/console", "/console/login.do");
    }

    @Test
    public void unmappedAboutAndContactPathsRequireAuthentication() throws Exception {
        assertRedirectsToLogin("/about", "/about/team", "/contact", "/contact/form");
    }

    @Test
    public void unusedWebjarsPathRequiresAuthentication() throws Exception {
        assertRedirectsToLogin("/webjars/jquery/jquery.js");
    }

    @Test
    public void onlyTheErrorPageItselfIsPublic() throws Exception {
        assertRedirectsToLogin("/error/500/detail");
    }

    @Test
    public void applicationPathsStillRequireAuthentication() throws Exception {
        assertRedirectsToLogin("/userFront", "/api/user/all");
    }

    private void assertRedirectsToLogin(String... paths) throws Exception {
        for (String path : paths) {
            mockMvc.perform(get(path))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrlPattern("**/index"));
        }
    }
}
