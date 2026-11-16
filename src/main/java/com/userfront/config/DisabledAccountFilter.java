package com.userfront.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Re-checks the signed-in user's account status on every request so that disabling (or deleting)
 * a user ends sessions that were opened before the change.
 */
public class DisabledAccountFilter extends OncePerRequestFilter {

    public static final String DISABLED_URL = "/index?disabled";

    private static final Logger LOG = LoggerFactory.getLogger(DisabledAccountFilter.class);

    private final UserDetailsService userDetailsService;
    private final UserDetailsChecker accountStatusChecker = new AccountStatusUserDetailsChecker();
    private final LogoutHandler logoutHandler = new CompositeLogoutHandler(
            new CookieClearingLogoutHandler("remember-me"),
            new SecurityContextLogoutHandler());

    public DisabledAccountFilter(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || isAccountActive(authentication.getName())) {
            chain.doFilter(request, response);
            return;
        }

        LOG.info("Ending session for inactive account {}", authentication.getName());
        logoutHandler.logout(request, response, authentication);
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.sendRedirect(request.getContextPath() + DISABLED_URL);
        }
    }

    private boolean isAccountActive(String username) {
        try {
            accountStatusChecker.check(userDetailsService.loadUserByUsername(username));
            return true;
        } catch (UsernameNotFoundException | AccountStatusException e) {
            return false;
        }
    }
}
