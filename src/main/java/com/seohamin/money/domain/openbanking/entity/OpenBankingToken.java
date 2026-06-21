package com.seohamin.money.domain.openbanking.entity;

import com.seohamin.money.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Duration;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사용자(user_seq_no)별로 발급받은 오픈뱅킹 OAuth 토큰. access/refresh 토큰을 보관하고
 * 만료 시 갱신한다.
 *
 * <p>주의: 데모 단계라 토큰을 평문 저장한다. 운영에서는 컬럼 암호화(AES/Jasypt 등)를 적용할 것.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "open_banking_token",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_obt_member_id", columnNames = "member_id"),
            @UniqueConstraint(name = "uk_obt_user_seq_no", columnNames = "user_seq_no")
        })
public class OpenBankingToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    public OpenBankingToken(
            final Member member,
            final String userSeqNo,
            final String accessToken,
            final String refreshToken,
            final String tokenType,
            final String scope,
            final Instant expiresAt
    ) {
        this.member = member;
        this.userSeqNo = userSeqNo;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scope = scope;
        this.expiresAt = expiresAt;
    }

    /** 토큰 갱신(refresh) 결과로 access/refresh 토큰과 만료시각을 교체한다. */
    public void renew(
            final String accessToken,
            final String refreshToken,
            final Instant expiresAt,
            final String scope
    ) {
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
    public boolean isExpiringWithin(final Duration buffer) {
        return Instant.now().plus(buffer).isAfter(expiresAt);
    }
}
