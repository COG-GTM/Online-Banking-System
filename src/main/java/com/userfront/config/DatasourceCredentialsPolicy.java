package com.userfront.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

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

    // Pool-specific properties are bound onto the DataSource after spring.datasource.*,
    // so they override the validated top-level values and must be checked too.
    static final List<Pool> POOLS = Arrays.asList(
            new Pool("spring.datasource.hikari.", "jdbc-url", "jdbcUrl"),
            new Pool("spring.datasource.tomcat.", "url"),
            new Pool("spring.datasource.dbcp2.", "url"));

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = resolve(environment, URL);
        String username = resolve(environment, USERNAME);
        String password = resolve(environment, PASSWORD);
        check("spring.datasource.", url, username, password);
        for (Pool pool : POOLS) {
            String poolUrl = pool.url(environment);
            String poolUsername = resolve(environment, pool.prefix + "username");
            String poolPassword = resolve(environment, pool.prefix + "password");
            if (poolUrl != null || poolUsername != null || poolPassword != null) {
                check(pool.prefix, poolUrl != null ? poolUrl : url,
                        poolUsername != null ? poolUsername : username,
                        poolPassword != null ? poolPassword : password);
            }
        }
    }

    private static void check(String prefix, String url, String username, String password) {
        if (url == null || isInMemory(url)) {
            return;
        }
        validate(prefix, username, password);
    }

    static void validate(String prefix, String username, String password) {
        if (isBlank(username)) {
            throw new InsecureDatasourceCredentialsException(prefix + "username is not set; provide DB_USERNAME");
        }
        if (PRIVILEGED_ACCOUNTS.contains(username.trim().toLowerCase(Locale.ROOT))) {
            throw new InsecureDatasourceCredentialsException(prefix + "username must be a least-privilege application account, not '" + username.trim() + "'");
        }
        if (isBlank(password)) {
            throw new InsecureDatasourceCredentialsException(prefix + "password is empty; provide DB_PASSWORD");
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
        final String prefix;
        private final List<String> urlKeys;

        Pool(String prefix, String... urlKeys) {
            this.prefix = prefix;
            this.urlKeys = Arrays.asList(urlKeys);
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
