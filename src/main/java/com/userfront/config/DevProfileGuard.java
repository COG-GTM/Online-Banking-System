package com.userfront.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Refuses to start with the "dev" profile (ddl-auto=update, SQL logging)
 * unless every configured JDBC URL points at a local database. Runs before any
 * bean is created, so Hibernate never touches a remote schema.
 */
public class DevProfileGuard implements EnvironmentPostProcessor, Ordered {

    static final String DEV_PROFILE = "dev";

    /** Properties from which the pool may take its JDBC URL; the pool-specific ones override spring.datasource.url. */
    static final String[] URL_PROPERTIES = {
            "spring.datasource.url",
            "spring.datasource.hikari.jdbc-url",
            "spring.datasource.tomcat.url",
            "spring.datasource.dbcp2.url",
    };

    /** Properties that connect without a JDBC URL this guard can check. */
    static final String[] UNCHECKABLE_PROPERTIES = {
            "spring.datasource.jndi-name",
            "spring.datasource.hikari.data-source-class-name",
            "spring.datasource.hikari.data-source-j-n-d-i",
    };

    private static final Set<String> LOCAL_HOSTS =
            new HashSet<>(Arrays.asList("localhost", "127.0.0.1", "::1"));

    private static final Pattern JDBC_HOST = Pattern.compile("^jdbc:[a-z0-9:]+://([^/?;]+)");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains(DEV_PROFILE)) {
            return;
        }
        Binder binder = Binder.get(environment);
        for (String name : UNCHECKABLE_PROPERTIES) {
            if (!binder.bind(name, String.class).orElse("").trim().isEmpty()) {
                throw refused(name + " is set");
            }
        }
        for (String name : URL_PROPERTIES) {
            String url = binder.bind(name, String.class).orElse(null);
            if (url != null && !isLocal(url)) {
                throw refused(name + " is not a local database");
            }
        }
        if (!binder.bind(URL_PROPERTIES[0], String.class).isBound()) {
            throw refused(URL_PROPERTIES[0] + " is not set");
        }
    }

    private static IllegalStateException refused(String reason) {
        return new IllegalStateException("The '" + DEV_PROFILE + "' profile enables ddl-auto=update and SQL "
                + "logging and may only be used with a MySQL database on localhost, 127.0.0.1 or ::1 ("
                + reason + ").");
    }

    static boolean isLocal(String url) {
        Matcher m = JDBC_HOST.matcher(url.trim().toLowerCase(Locale.ROOT));
        if (!m.find()) {
            return false;
        }
        for (String hostAndPort : m.group(1).split(",")) {
            String host = hostAndPort.startsWith("[")
                    ? hostAndPort.substring(1, Math.max(1, hostAndPort.indexOf(']')))
                    : hostAndPort.split(":")[0];
            if (!LOCAL_HOSTS.contains(host)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
