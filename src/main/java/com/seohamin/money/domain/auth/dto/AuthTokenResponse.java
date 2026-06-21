package com.seohamin.money.domain.auth.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {

    public static AuthTokenResponse bearer(final String accessToken, final long expiresInSeconds) {
        return new AuthTokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
