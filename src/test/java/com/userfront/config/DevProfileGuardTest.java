package com.userfront.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

public class DevProfileGuardTest {

    private final DevProfileGuard guard = new DevProfileGuard();

    private MockEnvironment env(String url, String... profiles) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profiles);
        env.setProperty("spring.datasource.url", url);
        return env;
    }

    @Test
    public void devProfileWithLocalDatabaseStarts() {
        guard.postProcessEnvironment(env("jdbc:mysql://localhost:3306/OnlineBankingSystem", "dev"), new SpringApplication());
        guard.postProcessEnvironment(env("jdbc:mysql://127.0.0.1:3307/obs?useSSL=false", "dev"), new SpringApplication());
    }

    @Test(expected = IllegalStateException.class)
    public void devProfileWithRemoteDatabaseIsRefused() {
        guard.postProcessEnvironment(env("jdbc:mysql://prod-db.bank.internal:3306/OnlineBankingSystem", "dev"), new SpringApplication());
    }

    @Test
    public void remoteDatabaseWithoutDevProfileIsNotChecked() {
        guard.postProcessEnvironment(env("jdbc:mysql://prod-db.bank.internal:3306/OnlineBankingSystem"), new SpringApplication());
    }

    @Test
    public void isLocalRecognisesOnlyLoopbackAndInMemoryUrls() {
        assertTrue(DevProfileGuard.isLocal("jdbc:mysql://localhost/db"));
        assertTrue(DevProfileGuard.isLocal("jdbc:mysql://[::1]:3306/db"));
        assertTrue(DevProfileGuard.isLocal("jdbc:h2:mem:test"));
        assertFalse(DevProfileGuard.isLocal("jdbc:mysql://localhost.evil.com/db"));
        assertFalse(DevProfileGuard.isLocal("jdbc:mysql://10.0.0.5:3306/db"));
        assertFalse(DevProfileGuard.isLocal("jdbc:mysql://db1,localhost/db"));
        assertFalse(DevProfileGuard.isLocal("jdbc:mysql://localhost,db2:3306/db"));
        assertTrue(DevProfileGuard.isLocal("jdbc:mysql://localhost:3306,127.0.0.1:3307/db"));
        assertFalse(DevProfileGuard.isLocal(""));
    }
}
