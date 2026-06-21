package com.seohamin.money.domain.openbanking.repository;

import com.seohamin.money.domain.openbanking.entity.OpenBankingToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenBankingTokenRepository extends JpaRepository<OpenBankingToken, Long> {

    Optional<OpenBankingToken> findByMemberId(Long memberId);

    Optional<OpenBankingToken> findByUserSeqNo(String userSeqNo);
}
