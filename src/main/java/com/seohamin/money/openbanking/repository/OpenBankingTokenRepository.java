package com.seohamin.money.openbanking.repository;

import com.seohamin.money.openbanking.domain.OpenBankingToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenBankingTokenRepository extends JpaRepository<OpenBankingToken, Long> {

    Optional<OpenBankingToken> findByUserSeqNo(String userSeqNo);
}
