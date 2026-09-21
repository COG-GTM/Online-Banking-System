package com.userfront.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String[] PUBLIC_MATCHERS = {
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/",
            "/about/**",
            "/contact/**",
            "/error/**/*",
            "/signup"
    };

    @Value("${app.cors.allowed-origins:}")
    private List<String> allowedOrigins;

    @Value("${app.security.require-https:false}")
    private boolean requireHttps;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(PUBLIC_MATCHERS).permitAll()
                .anyRequest().authenticated()
                .and()
                .cors().configurationSource(corsConfigurationSource())
                .and()
                .headers()
                .httpStrictTransportSecurity().includeSubDomains(true).maxAgeInSeconds(31536000)
                .and()
                .frameOptions().deny()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation().newSession()
                .and()
                .formLogin()
                .failureUrl("/index?error")
                .defaultSuccessUrl("/userFront")
                .loginPage("/index")
                .permitAll()
                .and()
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/index?logout")
                .deleteCookies("remember-me", "JSESSIONID")
                .permitAll();

        if (requireHttps) {
            http.requiresChannel().anyRequest().requiresSecure();
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(allowedOrigins);
            configuration.setAllowedMethods(Arrays.asList("GET", "POST"));
            configuration.setAllowedHeaders(Arrays.asList("Content-Type", "X-Requested-With", "X-CSRF-TOKEN"));
            configuration.setAllowCredentials(true);
            configuration.setMaxAge(3600L);
            source.registerCorsConfiguration("/api/**", configuration);
        } else {
            source.setCorsConfigurations(Collections.emptyMap());
        }

        return source;
    }
}
