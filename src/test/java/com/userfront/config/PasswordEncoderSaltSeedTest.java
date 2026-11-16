package com.userfront.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.security.Security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Reproduces a JVM whose default SecureRandom takes its whole state from a caller-supplied
 * seed (SHA1PRNG, e.g. Java 8 on Windows). Each new SecurityConfig stands in for an app restart.
 */
class PasswordEncoderSaltSeedTest {

    private static final String PROVIDER_NAME = "SeedOnlyDefaultTest";
    private static final int BCRYPT_SALT_PREFIX_LENGTH = 29;

    @BeforeEach
    void installSeedOnlyDefaultSecureRandom() {
        Security.insertProviderAt(new SeedOnlyProvider(), 1);
    }

    @AfterEach
    void removeSeedOnlyDefaultSecureRandom() {
        Security.removeProvider(PROVIDER_NAME);
    }

    @Test
    void saltsDifferAcrossRestartsOnSeedOnlyRuntime() {
        String first = SecurityConfig.passwordEncoder().encode("Summer2026!");
        String second = SecurityConfig.passwordEncoder().encode("Summer2026!");

        assertNotEquals(first.substring(0, BCRYPT_SALT_PREFIX_LENGTH),
                second.substring(0, BCRYPT_SALT_PREFIX_LENGTH));
    }

    @Test
    void encoderUsesStrength12AndVerifiesExistingHashes() {
        BCryptPasswordEncoder encoder = SecurityConfig.passwordEncoder();
        String legacyHash = new BCryptPasswordEncoder(12, new SecureRandom("salt".getBytes()))
                .encode("Summer2026!");

        assertTrue(encoder.encode("Summer2026!").startsWith("$2a$12$"));
        assertTrue(encoder.matches("Summer2026!", legacyHash));
    }

    public static final class SeedOnlyProvider extends Provider {
        SeedOnlyProvider() {
            super(PROVIDER_NAME, 1.0, "Default SecureRandom backed by SUN SHA1PRNG");
            put("SecureRandom.SHA1PRNG", SeedOnlySecureRandomSpi.class.getName());
        }
    }

    public static final class SeedOnlySecureRandomSpi extends SecureRandomSpi {
        private final SecureRandom delegate;

        public SeedOnlySecureRandomSpi() throws NoSuchAlgorithmException, NoSuchProviderException {
            delegate = SecureRandom.getInstance("SHA1PRNG", "SUN");
        }

        @Override
        protected void engineSetSeed(byte[] seed) {
            delegate.setSeed(seed);
        }

        @Override
        protected void engineNextBytes(byte[] bytes) {
            delegate.nextBytes(bytes);
        }

        @Override
        protected byte[] engineGenerateSeed(int numBytes) {
            return delegate.generateSeed(numBytes);
        }
    }
}
