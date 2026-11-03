package com.userfront.config;

import java.security.SecureRandom;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;

import com.userfront.service.UserServiceImpl.UserSecurityService;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

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
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/index?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .addLogoutHandler(this::expireSessionCookie)
                .deleteCookies("remember-me")
                .permitAll()
                .and()
                .rememberMe();
    }



    private void expireSessionCookie(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Cookie cookie = new Cookie(env.getProperty("server.servlet.session.cookie.name", "JSESSIONID"), null);
        String path = env.getProperty("server.servlet.session.cookie.path");
        if (!StringUtils.hasText(path)) {
            path = StringUtils.hasText(request.getContextPath()) ? request.getContextPath() : "/";
        }
        cookie.setPath(path);
        String domain = env.getProperty("server.servlet.session.cookie.domain");
        if (StringUtils.hasText(domain)) {
            cookie.setDomain(domain);
        }
        cookie.setHttpOnly(env.getProperty("server.servlet.session.cookie.http-only", Boolean.class, true));
        cookie.setSecure(env.getProperty("server.servlet.session.cookie.secure", Boolean.class, request.isSecure()));
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//    	 auth.inMemoryAuthentication().withUser("user").password("password").roles("USER"); //This is in-memory authentication
        auth.userDetailsService(userSecurityService).passwordEncoder(passwordEncoder());
    }


}
