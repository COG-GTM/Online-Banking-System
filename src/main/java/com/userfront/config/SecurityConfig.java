package com.userfront.config;

import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.userfront.service.UserServiceImpl.UserSecurityService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String REMEMBER_ME_COOKIE = "remember-me";

    private static final int MIN_REMEMBER_ME_KEY_LENGTH = 32;

    @Value("${app.security.remember-me.key:}")
    private String rememberMeKey;

    @Value("${app.security.remember-me.token-validity-seconds:86400}")
    private int rememberMeTokenValiditySeconds;

    @Value("${app.security.remember-me.secure-cookie:true}")
    private boolean rememberMeSecureCookie;

    @Autowired
    private Environment env;

    @Autowired
    private UserSecurityService userSecurityService;

    private static final String SALT = "salt"; // Salt should be protected carefully

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12, new SecureRandom(SALT.getBytes()));
    }

    private static final String[] PUBLIC_MATCHERS = {
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/",
            "/about/**",
            "/contact/**",
            "/error/**/*",
            "/console/**",
            "/signup"
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests().
//                antMatchers("/**").
                antMatchers(PUBLIC_MATCHERS).
                permitAll().anyRequest().authenticated();

        http
                .csrf().disable().cors().disable()
                .formLogin().failureUrl("/index?error").defaultSuccessUrl("/userFront").loginPage("/index").permitAll()
                .and()
                .logout().logoutRequestMatcher(new AntPathRequestMatcher("/logout")).logoutSuccessUrl("/index?logout").deleteCookies(REMEMBER_ME_COOKIE).permitAll()
                .and()
                .rememberMe()
                .key(resolveRememberMeKey())
                .rememberMeCookieName(REMEMBER_ME_COOKIE)
                .tokenValiditySeconds(rememberMeTokenValiditySeconds)
                .useSecureCookie(rememberMeSecureCookie)
                .userDetailsService(userSecurityService);
    }

    private String resolveRememberMeKey() {
        if (StringUtils.hasText(rememberMeKey)) {
            if (rememberMeKey.trim().length() < MIN_REMEMBER_ME_KEY_LENGTH) {
                throw new IllegalStateException("app.security.remember-me.key (REMEMBER_ME_KEY) must be at least "
                        + MIN_REMEMBER_ME_KEY_LENGTH + " characters; use a long random value, e.g. `openssl rand -base64 32`.");
            }
            return rememberMeKey;
        }
        LOG.warn("app.security.remember-me.key (REMEMBER_ME_KEY) is not set; using a random per-startup key, "
                + "so remember-me cookies will not survive a restart.");
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getEncoder().encodeToString(random);
    }



    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//    	 auth.inMemoryAuthentication().withUser("user").password("password").roles("USER"); //This is in-memory authentication
        auth.userDetailsService(userSecurityService).passwordEncoder(passwordEncoder());
    }


}
