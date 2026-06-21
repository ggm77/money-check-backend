package com.seohamin.money.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
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

    private final SecretKeySpec signingKey;
    private final long expirationSeconds;

    public JwtTokenProvider(final JwtProperties properties) {
        this.signingKey = new SecretKeySpec(
                properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.expirationSeconds = properties.accessTokenExpiration().toSeconds();
    }

    public String createToken(final Long memberId) {
        final long issuedAt = Instant.now().getEpochSecond();
        final long expiresAt = issuedAt + expirationSeconds;
        final String payload = ENCODER.encodeToString(
                ("{\"sub\":\"" + memberId + "\",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt + "}")
                        .getBytes(StandardCharsets.UTF_8));
        final String unsignedToken = HEADER + "." + payload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    public Optional<Long> parseMemberId(final String token) {
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
            if (!subjectMatcher.find() || !expirationMatcher.find()) {
                return Optional.empty();
            }

            final long expiresAt = Long.parseLong(expirationMatcher.group(1));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(subjectMatcher.group(1)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
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
}
