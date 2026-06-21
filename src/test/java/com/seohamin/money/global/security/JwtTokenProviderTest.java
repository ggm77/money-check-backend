package com.seohamin.money.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private final JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(
            "test-jwt-secret-must-be-at-least-32-bytes-long",
            Duration.ofHours(1)));

    @Test
    void createsAndParsesToken() {
        final String token = provider.createToken(42L);

        assertThat(provider.parseMemberId(token)).contains(42L);
    }

    @Test
    void rejectsTamperedToken() {
        final String token = provider.createToken(42L);
        final String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        assertThat(provider.parseMemberId(tampered)).isEmpty();
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        final JwtTokenProvider shortLivedProvider = new JwtTokenProvider(new JwtProperties(
                "test-jwt-secret-must-be-at-least-32-bytes-long",
                Duration.ofSeconds(1)));
        final String token = shortLivedProvider.createToken(42L);

        Thread.sleep(1100);

        assertThat(shortLivedProvider.parseMemberId(token)).isEmpty();
    }
}
