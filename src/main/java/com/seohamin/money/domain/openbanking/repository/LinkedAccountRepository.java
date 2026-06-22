package com.seohamin.money.domain.openbanking.repository;

import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    List<LinkedAccount> findByMemberId(Long memberId);

    Optional<LinkedAccount> findFirstByMemberIdOrderByIdAsc(Long memberId);

    Optional<LinkedAccount> findByMemberIdAndFintechUseNum(Long memberId, String fintechUseNum);

    Optional<LinkedAccount> findByFintechUseNum(String fintechUseNum);
}
