package com.seohamin.money.openbanking.support;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * OAuth {@code state} 발급/검증 저장소. 인메모리·TTL 기반이며, authorize 단계에서 발급한 state를
 * callback 단계에서 1회 소비해 CSRF를 방지한다. (단일 인스턴스 데모용; 다중 인스턴스라면 Redis 등으로 대체)
 */
@Component
public class OAuthStateStore {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Instant> issuedStates = new ConcurrentHashMap<>();

    /** 새 state를 발급하고 만료시각과 함께 저장한다. (32자, base64url) */
    public String issue() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        issuedStates.put(state, Instant.now().plus(TTL));
        return state;
    }

    /** state를 검증하고 즉시 제거한다. 존재하고 만료되지 않았으면 true. */
    public boolean consume(String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        purgeExpired();
        Instant expiresAt = issuedStates.remove(state);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        issuedStates.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
