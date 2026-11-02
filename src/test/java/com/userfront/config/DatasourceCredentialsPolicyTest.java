package com.userfront.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import com.userfront.config.DatasourceCredentialsPolicy.InsecureDatasourceCredentialsException;

public class DatasourceCredentialsPolicyTest {

    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/OnlineBankingSystem";

    private final DatasourceCredentialsPolicy policy = new DatasourceCredentialsPolicy();

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void committedApplicationPropertiesWithoutEnvironmentRefuseToStart() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            properties.load(in);
        }
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addLast(new PropertiesPropertySource("applicationConfig", properties));

        policy.postProcessEnvironment(environment, new SpringApplication());
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void rootWithEmptyPasswordIsRejected() {
        postProcess(MYSQL_URL, "root", "");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void rootWithPasswordIsRejected() {
        postProcess(MYSQL_URL, "root", "s3cr3t-Passw0rd");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void rootIsRejectedRegardlessOfCase() {
        postProcess(MYSQL_URL, " Root ", "s3cr3t-Passw0rd");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void applicationAccountWithEmptyPasswordIsRejected() {
        postProcess(MYSQL_URL, "obs_app", "   ");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void missingUsernameIsRejected() {
        postProcess(MYSQL_URL, null, "s3cr3t-Passw0rd");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void unresolvablePlaceholderIsRejected() {
        postProcess(MYSQL_URL, "${DB_USERNAME_NOT_SET}", "s3cr3t-Passw0rd");
    }

    @Test
    public void leastPrivilegeAccountWithPasswordIsAccepted() {
        postProcess(MYSQL_URL, "obs_app", "s3cr3t-Passw0rd");
    }

    @Test
    public void inMemoryTestDatabaseIsNotChecked() {
        postProcess("jdbc:h2:mem:testdb", "sa", "");
    }

    @Test
    public void noDatasourceUrlIsNotChecked() {
        postProcess(null, null, null);
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void hikariRootUsernameOverrideIsRejected() {
        postProcess(MYSQL_URL, "obs_app", "s3cr3t-Passw0rd", "spring.datasource.hikari.username", "root");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void hikariEmptyPasswordOverrideIsRejected() {
        postProcess(MYSQL_URL, "obs_app", "s3cr3t-Passw0rd", "spring.datasource.hikari.password", "");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void hikariJdbcUrlOverrideOfInMemoryUrlIsChecked() {
        postProcess("jdbc:h2:mem:testdb", "root", "", "spring.datasource.hikari.jdbc-url", MYSQL_URL);
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void tomcatPoolRootUsernameOverrideIsRejected() {
        postProcess(MYSQL_URL, "obs_app", "s3cr3t-Passw0rd", "spring.datasource.tomcat.username", "root");
    }

    @Test(expected = InsecureDatasourceCredentialsException.class)
    public void dbcp2PoolRootUsernameOverrideIsRejected() {
        postProcess(MYSQL_URL, "obs_app", "s3cr3t-Passw0rd", "spring.datasource.dbcp2.username", "root");
    }

    @Test
    public void hikariLeastPrivilegeOverrideIsAccepted() {
        postProcess(MYSQL_URL, "obs_app", "s3cr3t-Passw0rd", "spring.datasource.hikari.username", "obs_pool");
    }

    private void postProcess(String url, String username, String password, String... extra) {
        Map<String, Object> values = new HashMap<>();
        put(values, DatasourceCredentialsPolicy.URL, url);
        put(values, DatasourceCredentialsPolicy.USERNAME, username);
        put(values, DatasourceCredentialsPolicy.PASSWORD, password);
        for (int i = 0; i < extra.length; i += 2) {
            values.put(extra[i], extra[i + 1]);
        }
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", values));
        policy.postProcessEnvironment(environment, new SpringApplication());
    }

    private static void put(Map<String, Object> values, String key, String value) {
        if (value != null) {
            values.put(key, value);
        }
    }
}
