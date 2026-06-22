package com.seohamin.money.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = ENCODER.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    private static final Pattern SUBJECT_PATTERN = Pattern.compile("\"sub\":\"(\\d+)\"");
    private static final Pattern EXPIRATION_PATTERN = Pattern.compile("\"exp\":(\\d+)");
    private static final Pattern TOKEN_TYPE_PATTERN = Pattern.compile("\"tokenType\":\"(ACCESS|REFRESH)\"");

    private final SecretKeySpec signingKey;
    private final long accessTokenExpirationSeconds;
    private final long refreshTokenExpirationSeconds;

    public JwtTokenProvider(final JwtProperties properties) {
        this.signingKey = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.accessTokenExpirationSeconds = properties.accessTokenExpiration().toSeconds();
        this.refreshTokenExpirationSeconds = properties.refreshTokenExpiration().toSeconds();
    }

    public String createAccessToken(final Long memberId) {
        return createToken(memberId, TokenType.ACCESS, accessTokenExpirationSeconds);
    }

    public String createRefreshToken(final Long memberId) {
        return createToken(memberId, TokenType.REFRESH, refreshTokenExpirationSeconds);
    }

    public Optional<Long> parseAccessToken(final String token) {
        return parseMemberId(token, TokenType.ACCESS);
    }

    public Optional<Long> parseRefreshToken(final String token) {
        return parseMemberId(token, TokenType.REFRESH);
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    private String createToken(
            final Long memberId,
            final TokenType tokenType,
            final long expirationSeconds
    ) {
        final long issuedAt = Instant.now().getEpochSecond();
        final long expiresAt = issuedAt + expirationSeconds;
        final String payload = ENCODER.encodeToString(
                ("{\"sub\":\"" + memberId
                        + "\",\"tokenType\":\"" + tokenType
                        + "\",\"iat\":" + issuedAt
                        + ",\"exp\":" + expiresAt
                        + ",\"jti\":\"" + UUID.randomUUID() + "\"}")
                        .getBytes(StandardCharsets.UTF_8));
        final String unsignedToken = HEADER + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private Optional<Long> parseMemberId(final String token, final TokenType expectedTokenType) {
        try {
            final String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || !HEADER.equals(parts[0])) {
                return Optional.empty();
            }

            final byte[] expected = signBytes(parts[0] + "." + parts[1]);
            final byte[] actual = DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                return Optional.empty();
            }

            final String payload = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
            final Matcher subjectMatcher = SUBJECT_PATTERN.matcher(payload);
            final Matcher expirationMatcher = EXPIRATION_PATTERN.matcher(payload);
            final Matcher tokenTypeMatcher = TOKEN_TYPE_PATTERN.matcher(payload);
            if (!subjectMatcher.find() || !expirationMatcher.find() || !tokenTypeMatcher.find()) {
                return Optional.empty();
            }
            if (!expectedTokenType.name().equals(tokenTypeMatcher.group(1))) {
                return Optional.empty();
            }

            final long expiresAt = Long.parseLong(expirationMatcher.group(1));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(subjectMatcher.group(1)));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private String sign(final String value) {
        return ENCODER.encodeToString(signBytes(value));
    }

    private byte[] signBytes(final String value) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 서명 생성에 실패했습니다.", exception);
        }
    }

    private enum TokenType {
        ACCESS,
        REFRESH
    }
}
