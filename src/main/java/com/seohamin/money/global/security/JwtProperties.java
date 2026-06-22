package com.seohamin.money.global.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt")
public record JwtProperties(
        String secret,
        Duration accessTokenExpiration,
        Duration refreshTokenExpiration
) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("jwt.secret 설정이 필요합니다.");
        }
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("jwt.secret은 32바이트 이상이어야 합니다.");
        }
        if (accessTokenExpiration == null || accessTokenExpiration.isNegative()
                || accessTokenExpiration.isZero()) {
            throw new IllegalArgumentException("jwt.access-token-expiration은 양수여야 합니다.");
        }
        if (refreshTokenExpiration == null || refreshTokenExpiration.isNegative()
                || refreshTokenExpiration.isZero()) {
            throw new IllegalArgumentException("jwt.refresh-token-expiration은 양수여야 합니다.");
        }
        if (refreshTokenExpiration.compareTo(accessTokenExpiration) <= 0) {
            throw new IllegalArgumentException("refresh token 만료시간은 access token보다 길어야 합니다.");
        }
    }
}
