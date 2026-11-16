package com.userfront.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SameSiteCookieIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void sessionAndCsrfCookiesAreSameSiteLax() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/userFront").openConnection();
        connection.setInstanceFollowRedirects(false);

        List<String> setCookies = connection.getHeaderFields().get("Set-Cookie");

        assertThat(setCookies).anyMatch(c -> c.startsWith("JSESSIONID=")).anyMatch(c -> c.startsWith("XSRF-TOKEN="));
        assertThat(setCookies).allMatch(c -> c.contains("SameSite=Lax"));
    }
}
