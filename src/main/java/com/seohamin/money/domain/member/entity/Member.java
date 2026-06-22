package com.seohamin.money.domain.member.entity;

import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.entity.OpenBankingToken;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "member",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_email", columnNames = "email"))
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @OneToMany(
            mappedBy = "member",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<LinkedAccount> linkedAccounts = new ArrayList<>();

    @OneToOne(
            mappedBy = "member",
            fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE,
            orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private OpenBankingToken openBankingToken;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public Member(final String email, final String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public void updateEmail(final String email) {
        this.email = email;
    }

    public void updatePasswordHash(final String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
