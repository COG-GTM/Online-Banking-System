package com.userfront.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

public final class CorsAllowList {

    static final List<String> ALLOWED_METHODS = Collections.unmodifiableList(
            Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"));

    static final List<String> ALLOWED_HEADERS = Collections.unmodifiableList(
            Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"));

    static final long MAX_AGE_SECONDS = 3600L;

    private CorsAllowList() {}

    public static List<String> parseOrigins(String raw) {
        List<String> origins = new ArrayList<>();
        if (raw == null) {
            return origins;
        }
        for (String entry : raw.split(",")) {
            String origin = entry.trim();
            if (origin.isEmpty()) {
                continue;
            }
            String normalized = normalizeOrigin(origin);
            if (!origins.contains(normalized)) {
                origins.add(normalized);
            }
        }
        return origins;
    }

    static String normalizeOrigin(String origin) {
        if (CorsConfiguration.ALL.equals(origin) || "null".equalsIgnoreCase(origin) || origin.contains("*")) {
            throw new IllegalArgumentException("Wildcard or null CORS origin is not allowed: " + origin);
        }
        URI uri;
        try {
            uri = new URI(origin);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid CORS origin: " + origin, e);
        }
        String scheme = uri.getScheme() == null ? null : uri.getScheme().toLowerCase(Locale.ROOT);
        boolean hasPath = uri.getRawPath() != null && !uri.getRawPath().isEmpty();
        if (!("https".equals(scheme) || "http".equals(scheme))
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || hasPath
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException(
                    "CORS origin must be scheme://host[:port] with no path, query or credentials: " + origin);
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return uri.getPort() == -1 ? scheme + "://" + host : scheme + "://" + host + ":" + uri.getPort();
    }

    public static CorsConfigurationSource source(List<String> origins, boolean allowCredentials) {
        if (origins.isEmpty()) {
            throw new IllegalArgumentException("At least one CORS origin is required");
        }
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(new ArrayList<>(origins));
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(MAX_AGE_SECONDS);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
