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

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = resolve(environment, URL);
        if (url == null || isInMemory(url)) {
            return;
        }
        validate(resolve(environment, USERNAME), resolve(environment, PASSWORD));
    }

    static void validate(String username, String password) {
        if (isBlank(username)) {
            throw new InsecureDatasourceCredentialsException(USERNAME + " is not set; provide DB_USERNAME");
        }
        if (PRIVILEGED_ACCOUNTS.contains(username.trim().toLowerCase(Locale.ROOT))) {
            throw new InsecureDatasourceCredentialsException(USERNAME + " must be a least-privilege application account, not '" + username.trim() + "'");
        }
        if (isBlank(password)) {
            throw new InsecureDatasourceCredentialsException(PASSWORD + " is empty; provide DB_PASSWORD");
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

    public static class InsecureDatasourceCredentialsException extends IllegalStateException {
        InsecureDatasourceCredentialsException(String message) {
            super("Refusing to start with insecure datasource credentials: " + message);
        }
    }
}
