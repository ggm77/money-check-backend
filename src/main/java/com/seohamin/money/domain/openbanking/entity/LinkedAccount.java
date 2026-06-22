package com.seohamin.money.domain.openbanking.entity;

import com.seohamin.money.domain.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사용자가 오픈뱅킹에 등록한 출금/조회용 계좌. 사용자정보조회(user/me) 응답의 res_list 한 건에 대응하며,
 * 잔액조회의 키가 되는 {@code fintech_use_num}(핀테크이용번호)을 보관한다.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        name = "linked_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_la_fintech_use_num", columnNames = "fintech_use_num"),
        indexes = {
            @Index(name = "idx_la_member_id", columnList = "member_id"),
            @Index(name = "idx_la_user_seq_no", columnList = "user_seq_no")
        })
public class LinkedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(name = "user_seq_no", nullable = false, length = 20)
    private String userSeqNo;

    @Column(name = "fintech_use_num", nullable = false, length = 24)
    private String fintechUseNum;

    @Column(name = "bank_code_std", length = 3)
    private String bankCodeStd;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_alias", length = 100)
    private String accountAlias;

    @Column(name = "account_num_masked", length = 32)
    private String accountNumMasked;

    @Column(name = "account_holder_name", length = 20)
    private String accountHolderName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public LinkedAccount(
            final Member member,
            final String userSeqNo,
            final String fintechUseNum,
            final String bankCodeStd,
            final String bankName,
            final String accountAlias,
            final String accountNumMasked,
            final String accountHolderName
    ) {
        this.member = member;
        this.userSeqNo = userSeqNo;
        this.fintechUseNum = fintechUseNum;
        this.bankCodeStd = bankCodeStd;
        this.bankName = bankName;
        this.accountAlias = accountAlias;
        this.accountNumMasked = accountNumMasked;
        this.accountHolderName = accountHolderName;
    }
}
