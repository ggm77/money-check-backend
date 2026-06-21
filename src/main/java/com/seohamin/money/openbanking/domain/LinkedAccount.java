package com.seohamin.money.openbanking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * 사용자가 오픈뱅킹에 등록한 출금/조회용 계좌. 사용자정보조회(user/me) 응답의 res_list 한 건에 대응하며,
 * 잔액조회의 키가 되는 {@code fintech_use_num}(핀테크이용번호)을 보관한다.
 */
@Entity
@Table(
        name = "linked_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_la_fintech_use_num", columnNames = "fintech_use_num"),
        indexes = @Index(name = "idx_la_user_seq_no", columnList = "user_seq_no"))
public class LinkedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LinkedAccount() {
        // for JPA
    }

    public LinkedAccount(
            String userSeqNo,
            String fintechUseNum,
            String bankCodeStd,
            String bankName,
            String accountAlias,
            String accountNumMasked,
            String accountHolderName) {
        this.userSeqNo = userSeqNo;
        this.fintechUseNum = fintechUseNum;
        this.bankCodeStd = bankCodeStd;
        this.bankName = bankName;
        this.accountAlias = accountAlias;
        this.accountNumMasked = accountNumMasked;
        this.accountHolderName = accountHolderName;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUserSeqNo() {
        return userSeqNo;
    }

    public String getFintechUseNum() {
        return fintechUseNum;
    }

    public String getBankCodeStd() {
        return bankCodeStd;
    }

    public String getBankName() {
        return bankName;
    }

    public String getAccountAlias() {
        return accountAlias;
    }

    public String getAccountNumMasked() {
        return accountNumMasked;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
