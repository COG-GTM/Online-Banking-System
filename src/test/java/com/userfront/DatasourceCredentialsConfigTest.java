package com.userfront;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class DatasourceCredentialsConfigTest {

    private static Properties applicationProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream in = DatasourceCredentialsConfigTest.class.getResourceAsStream("/application.properties")) {
            properties.load(in);
        }
        return properties;
    }

    @Test
    public void datasourceUsernameComesFromEnvironmentWithoutDefault() throws IOException {
        assertEquals("${DB_USERNAME}", applicationProperties().getProperty("spring.datasource.username"));
    }

    @Test
    public void datasourcePasswordComesFromEnvironmentWithoutDefault() throws IOException {
        assertEquals("${DB_PASSWORD}", applicationProperties().getProperty("spring.datasource.password"));
    }
}
