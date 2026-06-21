package com.seohamin.money.openbanking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;

/**
 * 사용자(user_seq_no)별로 발급받은 오픈뱅킹 OAuth 토큰. access/refresh 토큰을 보관하고
 * 만료 시 갱신한다.
 *
 * <p>주의: 데모 단계라 토큰을 평문 저장한다. 운영에서는 컬럼 암호화(AES/Jasypt 등)를 적용할 것.
 */
@Entity
@Table(
        name = "open_banking_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_obt_user_seq_no", columnNames = "user_seq_no"))
public class OpenBankingToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_seq_no", nullable = false, length = 20)
    private String userSeqNo;

    @Column(name = "access_token", nullable = false, length = 1024)
    private String accessToken;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "token_type", length = 20)
    private String tokenType;

    @Column(name = "scope", length = 100)
    private String scope;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OpenBankingToken() {
        // for JPA
    }

    public OpenBankingToken(
            String userSeqNo,
            String accessToken,
            String refreshToken,
            String tokenType,
            String scope,
            Instant expiresAt) {
        this.userSeqNo = userSeqNo;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /** 토큰 갱신(refresh) 결과로 access/refresh 토큰과 만료시각을 교체한다. */
    public void renew(String accessToken, String refreshToken, Instant expiresAt, String scope) {
        this.accessToken = accessToken;
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.refreshToken = refreshToken;
        }
        this.expiresAt = expiresAt;
        if (scope != null && !scope.isBlank()) {
            this.scope = scope;
        }
    }

    /** 지금으로부터 {@code buffer} 이내에 만료되면(또는 이미 만료면) true. */
    public boolean isExpiringWithin(Duration buffer) {
        return Instant.now().plus(buffer).isAfter(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public String getUserSeqNo() {
        return userSeqNo;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
