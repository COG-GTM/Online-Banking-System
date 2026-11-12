package com.userfront.support;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Points the application at a throwaway MySQL 5.7 container, shared by every test context in the JVM.
 */
public class MySqlContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:5.7"))
            .withDatabaseName("OnlineBankingSystem")
            .withUrlParam("useSSL", "false");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        synchronized (MYSQL) {
            if (!MYSQL.isRunning()) {
                MYSQL.start();
            }
        }
        TestPropertyValues.of(
                "spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "spring.datasource.username=" + MYSQL.getUsername(),
                "spring.datasource.password=" + MYSQL.getPassword(),
                "spring.jpa.show-sql=false"
        ).applyTo(context.getEnvironment());
    }
}
