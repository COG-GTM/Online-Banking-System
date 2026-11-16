package com.userfront.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ClassUtils;

/**
 * Refuses to start the application when the datasource would connect with a
 * missing, superuser or password-less account. Runs after application.properties
 * has been loaded, so it sees the effective (environment-resolved) values.
 */
public class DatasourceCredentialsPolicy implements EnvironmentPostProcessor, Ordered {

    static final String URL = "spring.datasource.url";
    static final String USERNAME = "spring.datasource.username";
    static final String PASSWORD = "spring.datasource.password";

    private static final List<String> PRIVILEGED_ACCOUNTS = Arrays.asList("root", "mysql.sys", "mysql.session", "admin");

    private static final List<String> IN_MEMORY_URL_PREFIXES = Arrays.asList("jdbc:h2:mem:", "jdbc:hsqldb:mem:", "jdbc:derby:memory:");

    static final String TYPE = "spring.datasource.type";

    // A JNDI datasource is obtained from the container; url/username/password are not used.
    static final String JNDI_NAME = "spring.datasource.jndi-name";

    // Spring Boot binds the selected pool's spring.datasource.<pool>.* properties after
    // spring.datasource.*, so they override the top-level values; only the effective
    // (merged) configuration is validated. Listed in Boot's auto-detection order.
    static final List<Pool> POOLS = Arrays.asList(
            new Pool("com.zaxxer.hikari.HikariDataSource", "spring.datasource.hikari.",
                    Arrays.asList("jdbc-url", "jdbcUrl"), Arrays.asList("data-source-class-name", "dataSourceClassName")),
            new Pool("org.apache.tomcat.jdbc.pool.DataSource", "spring.datasource.tomcat.",
                    Arrays.asList("url"), Collections.<String>emptyList()),
            new Pool("org.apache.commons.dbcp2.BasicDataSource", "spring.datasource.dbcp2.",
                    Arrays.asList("url"), Collections.<String>emptyList()));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isBlank(resolve(environment, JNDI_NAME))) {
            return;
        }
        Pool pool = activePool(resolve(environment, TYPE), application.getClassLoader());
        String url = pool != null ? pool.url(environment) : null;
        if (url == null) {
            url = resolve(environment, URL);
        }
        if (url == null || (isInMemory(url) && !usesDataSourceClass(environment, pool))) {
            return;
        }
        String usernameKey = effectiveKey(environment, pool, "username", USERNAME);
        String passwordKey = effectiveKey(environment, pool, "password", PASSWORD);
        validate(usernameKey, resolve(environment, usernameKey), passwordKey, resolve(environment, passwordKey));
    }

    // The pool-specific value wins when present, so the overridden top-level value is never resolved.
    private static String effectiveKey(ConfigurableEnvironment environment, Pool pool, String poolProperty, String topLevelKey) {
        if (pool != null && environment.containsProperty(pool.prefix + poolProperty)) {
            return pool.prefix + poolProperty;
        }
        return topLevelKey;
    }

    // Hikari ignores jdbc-url when a dataSourceClassName is configured, so the URL can't prove an in-memory database.
    private static boolean usesDataSourceClass(ConfigurableEnvironment environment, Pool pool) {
        if (pool == null || pool.dataSourceClassKeys.isEmpty()) {
            return false;
        }
        for (String key : pool.dataSourceClassKeys) {
            if (!isBlank(resolve(environment, pool.prefix + key))) {
                return true;
            }
        }
        return false;
    }

    static Pool activePool(String type, ClassLoader classLoader) {
        for (Pool pool : POOLS) {
            if (type != null ? pool.className.equals(type.trim()) : ClassUtils.isPresent(pool.className, classLoader)) {
                return pool;
            }
        }
        return null;
    }

    static void validate(String usernameKey, String username, String passwordKey, String password) {
        if (isBlank(username)) {
            throw new InsecureDatasourceCredentialsException(usernameKey + " is not set; provide DB_USERNAME");
        }
        if (PRIVILEGED_ACCOUNTS.contains(username.trim().toLowerCase(Locale.ROOT))) {
            throw new InsecureDatasourceCredentialsException(usernameKey + " must be a least-privilege application account, not '" + username.trim() + "'");
        }
        if (isBlank(password)) {
            throw new InsecureDatasourceCredentialsException(passwordKey + " is empty; provide DB_PASSWORD");
        }
    }

    private static String resolve(ConfigurableEnvironment environment, String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            throw new InsecureDatasourceCredentialsException(unresolvedPlaceholder.getMessage());
        }
    }

    private static boolean isInMemory(String url) {
        for (String prefix : IN_MEMORY_URL_PREFIXES) {
            if (url.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    static final class Pool {
        final String className;
        final String prefix;
        private final List<String> urlKeys;
        private final List<String> dataSourceClassKeys;

        Pool(String className, String prefix, List<String> urlKeys, List<String> dataSourceClassKeys) {
            this.className = className;
            this.prefix = prefix;
            this.urlKeys = urlKeys;
            this.dataSourceClassKeys = dataSourceClassKeys;
        }

        String url(ConfigurableEnvironment environment) {
            for (String key : urlKeys) {
                String value = resolve(environment, prefix + key);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
    }

    public static class InsecureDatasourceCredentialsException extends IllegalStateException {
        InsecureDatasourceCredentialsException(String message) {
            super("Refusing to start with insecure datasource credentials: " + message);
        }
    }
}
