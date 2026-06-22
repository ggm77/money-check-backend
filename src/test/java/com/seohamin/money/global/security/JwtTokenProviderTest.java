package com.seohamin.money.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
            "test-jwt-secret-must-be-at-least-32-bytes-long",
            Duration.ofHours(1),
            Duration.ofDays(14)));

    @Test
    void createsAndParsesAccessToken() {
        final String token = provider.createAccessToken(42L);

        assertThat(provider.parseAccessToken(token)).contains(42L);
        assertThat(provider.parseRefreshToken(token)).isEmpty();
    }

    @Test
    void createsAndParsesRefreshToken() {
        final String token = provider.createRefreshToken(42L);

        assertThat(provider.parseRefreshToken(token)).contains(42L);
        assertThat(provider.parseAccessToken(token)).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        final String token = provider.createAccessToken(42L);
        final String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(provider.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    void rejectsExpiredAccessToken() throws InterruptedException {
        final JwtTokenProvider shortLivedProvider = new JwtTokenProvider(new JwtProperties(
                "test-jwt-secret-must-be-at-least-32-bytes-long",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)));
        final String token = shortLivedProvider.createAccessToken(42L);

        Thread.sleep(1100);

        assertThat(shortLivedProvider.parseAccessToken(token)).isEmpty();
    }
}
