package com.seohamin.money.domain.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        long refreshTokenExpiresInSeconds) {

    public static AuthTokenResponse bearer(
            final String accessToken,
            final String refreshToken,
            final long expiresInSeconds,
            final long refreshTokenExpiresInSeconds
    ) {
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresInSeconds,
                refreshTokenExpiresInSeconds);
    }
}
