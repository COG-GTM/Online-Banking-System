package com.userfront.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Refuses to start with the "dev" profile (ddl-auto=update, SQL logging)
 * unless the datasource is a local database. Runs before any bean is created,
 * so Hibernate never touches a remote schema.
 */
public class DevProfileGuard implements EnvironmentPostProcessor, Ordered {

    static final String DEV_PROFILE = "dev";

    private static final Set<String> LOCAL_HOSTS =
            new HashSet<>(Arrays.asList("localhost", "127.0.0.1", "::1"));

    private static final Pattern JDBC_HOST = Pattern.compile("^jdbc:[a-z0-9:]+://([^/?;]+)");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!Arrays.asList(environment.getActiveProfiles()).contains(DEV_PROFILE)) {
            return;
        }
        String url = environment.getProperty("spring.datasource.url", "");
        String jndiName = environment.getProperty("spring.datasource.jndi-name", "");
        if (!jndiName.trim().isEmpty() || !isLocal(url)) {
            throw new IllegalStateException("The '" + DEV_PROFILE + "' profile enables ddl-auto=update and SQL "
                    + "logging and may only be used with a local database (localhost, 127.0.0.1, ::1 or an "
                    + "in-memory H2 database) configured through spring.datasource.url, not through "
                    + "spring.datasource.jndi-name.");
        }
    }

    static boolean isLocal(String url) {
        String u = url.trim().toLowerCase(Locale.ROOT);
        if (u.startsWith("jdbc:h2:mem:")) {
            return true;
        }
        Matcher m = JDBC_HOST.matcher(u);
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
