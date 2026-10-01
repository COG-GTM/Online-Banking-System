package com.userfront.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    static final List<String> ALLOWED_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");

    static final List<String> ALLOWED_HEADERS = Arrays.asList(
            "Authorization", "Content-Type", "Accept", "X-Requested-With");

    static final long MAX_AGE_SECONDS = 3600L;

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${userfront.cors.allowed-origins:}") String allowedOrigins,
            @Value("${userfront.cors.allow-credentials:false}") boolean allowCredentials) {
        return buildSource(parseOrigins(allowedOrigins), allowCredentials);
    }

    static CorsConfigurationSource buildSource(List<String> origins, boolean allowCredentials) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (origins.isEmpty()) {
            return source;
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(MAX_AGE_SECONDS);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    static List<String> parseOrigins(String raw) {
        List<String> origins = new ArrayList<>();
        if (raw == null) {
            return origins;
        }
        for (String entry : raw.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String origin = normalizeOrigin(trimmed);
            if (!origins.contains(origin)) {
                origins.add(origin);
            }
        }
        return origins;
    }

    static String normalizeOrigin(String origin) {
        if (origin.contains("*") || "null".equalsIgnoreCase(origin)) {
            throw new IllegalArgumentException("Wildcard or null CORS origin is not allowed: " + origin);
        }
        URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid CORS origin: " + origin, e);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String path = uri.getRawPath();
        if (!("https".equals(scheme) || "http".equals(scheme))
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || (path != null && !path.isEmpty())
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "CORS origin must be scheme://host[:port] without path, query or credentials: " + origin);
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return uri.getPort() == -1 ? scheme + "://" + host : scheme + "://" + host + ":" + uri.getPort();
    }
}
