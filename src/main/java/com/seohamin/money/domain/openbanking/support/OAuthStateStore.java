package com.seohamin.money.domain.openbanking.support;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
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

    private final Map<String, IssuedState> issuedStates = new ConcurrentHashMap<>();

    /** 로그인 사용자의 식별자를 바인딩한 state를 발급한다. (32자, base64url) */
    public String issue(final Long memberId) {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        issuedStates.put(state, new IssuedState(memberId, Instant.now().plus(TTL)));
        return state;
    }

    /** state를 검증하고 즉시 제거한 뒤 authorize 요청을 시작한 사용자 ID를 반환한다. */
    public Optional<Long> consume(final String state) {
        if (state == null || state.isBlank()) {
            return Optional.empty();
        }
        purgeExpired();
        final IssuedState issuedState = issuedStates.remove(state);
        if (issuedState == null || !issuedState.expiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(issuedState.memberId());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        issuedStates.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record IssuedState(Long memberId, Instant expiresAt) {}
}
