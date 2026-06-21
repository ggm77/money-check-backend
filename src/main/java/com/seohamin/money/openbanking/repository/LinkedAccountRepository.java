package com.seohamin.money.openbanking.repository;

import com.seohamin.money.openbanking.domain.LinkedAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkedAccountRepository extends JpaRepository<LinkedAccount, Long> {

    List<LinkedAccount> findByUserSeqNo(String userSeqNo);

    Optional<LinkedAccount> findFirstByUserSeqNoOrderByIdAsc(String userSeqNo);

    Optional<LinkedAccount> findByUserSeqNoAndFintechUseNum(String userSeqNo, String fintechUseNum);

    boolean existsByFintechUseNum(String fintechUseNum);
}
