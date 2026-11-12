package com.userfront;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.Filter;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.CorsFilter;

import com.userfront.support.AbstractIntegrationTest;

/**
 * Pins down what SecurityConfig and RequestFilter do today, so the Security 5.8 / 6 rewrite can be checked against it.
 */
public class SecurityFilterChainTest extends AbstractIntegrationTest {

    private static final String LOGIN_PAGE = "http://localhost/index";

    private static final String[] PROTECTED_PAGES = {
            "/userFront",
            "/account/primaryAccount",
            "/account/savingsAccount",
            "/account/deposit",
            "/account/withdraw",
            "/transfer/betweenAccounts",
            "/transfer/recipient",
            "/transfer/toSomeoneElse",
            "/appointment/create",
            "/user/profile",
            "/api/user/all",
            "/api/appointment/all",
            "/some/unmapped/path"
    };

    private static final String[] ADMIN_READ_ENDPOINTS = {
            "/api/user/all",
            "/api/appointment/all",
            "/api/user/primary/transaction?username=nobody",
            "/api/user/savings/transaction?username=nobody"
    };

    private static final String[] ADMIN_WRITE_ENDPOINTS = {
            "/api/user/nobody/enable",
            "/api/user/nobody/disable",
            "/api/appointment/-1/confirm"
    };

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Test
    public void singleFilterChainHasFormLoginLogoutRememberMeAndCsrfButNoCors() {
        List<SecurityFilterChain> chains = springSecurityFilterChain.getFilterChains();
        assertThat(chains).hasSize(1);

        List<Class<?>> filters = chains.get(0).getFilters().stream()
                .map(Filter::getClass)
                .collect(Collectors.toList());
        assertThat(filters).contains(
                LogoutFilter.class,
                UsernamePasswordAuthenticationFilter.class,
                RememberMeAuthenticationFilter.class,
                ExceptionTranslationFilter.class,
                CsrfFilter.class);
        assertThat(filters).doesNotContain(CorsFilter.class);
    }

    @Test
    public void publicPagesAreReachableAnonymously() throws Exception {
        mockMvc.perform(get("/")).andExpect(redirectedUrl("/index"));
        mockMvc.perform(get("/index")).andExpect(status().isOk()).andExpect(view().name("index"));
        mockMvc.perform(get("/signup")).andExpect(status().isOk()).andExpect(view().name("signup"));
        mockMvc.perform(get("/css/bootstrap.css")).andExpect(status().isOk());
    }

    @Test
    public void publicMatchersWithoutHandlersAreNotSentToLogin() throws Exception {
        for (String path : new String[] {"/about", "/contact", "/console/anything", "/webjars/x.js", "/images/missing.png"}) {
            mockMvc.perform(get(path)).andExpect(status().isNotFound());
        }
    }

    @Test
    public void protectedPagesRedirectAnonymousUsersToLogin() throws Exception {
        for (String path : PROTECTED_PAGES) {
            mockMvc.perform(get(path))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl(LOGIN_PAGE));
        }
    }

    @Test
    @WithMockUser(roles = "USER")
    public void adminApiIsForbiddenForRoleUser() throws Exception {
        for (String path : ADMIN_READ_ENDPOINTS) {
            mockMvc.perform(get(path)).andExpect(status().isForbidden());
        }
        for (String path : ADMIN_WRITE_ENDPOINTS) {
            mockMvc.perform(post(path).with(csrf())).andExpect(status().isForbidden());
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void adminApiIsAllowedForRoleAdmin() throws Exception {
        mockMvc.perform(get("/api/user/all")).andExpect(status().isOk());
        mockMvc.perform(get("/api/appointment/all")).andExpect(status().isOk());
    }

    @Test
    public void loginFormPostWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/index").param("username", "nobody").param("password", "wrong"))
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    public void loginFormPostWithCsrfTokenIsProcessed() throws Exception {
        mockMvc.perform(post("/index").with(csrf()).param("username", "nobody").param("password", "wrong"))
                .andExpect(redirectedUrl("/index?error"))
                .andExpect(unauthenticated());
    }

    @Test
    @WithMockUser
    public void logoutWorksOverGetAndClearsRememberMeCookie() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(redirectedUrl("/index?logout"))
                .andExpect(cookie().maxAge("remember-me", 0))
                .andExpect(unauthenticated());
    }

    @Test
    public void requestFilterAddsCorsHeadersAndAnswersPreflightWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/userFront"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", "POST,GET,DELETE"))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("x-xsrf-token")));
    }
}
