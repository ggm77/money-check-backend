package com.seohamin.money.openbanking.service;

import com.seohamin.money.openbanking.client.OpenBankingClient;
import com.seohamin.money.openbanking.client.dto.BalanceApiResponse;
import com.seohamin.money.openbanking.domain.LinkedAccount;
import com.seohamin.money.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.openbanking.support.BankTranIdGenerator;
import com.seohamin.money.openbanking.web.dto.BalanceResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

/** 잔액조회 오케스트레이션: 유효 토큰 확보 → 요청 식별자 생성 → 잔액 API 호출 → 응답 매핑. */
@Service
public class BalanceService {

    private static final DateTimeFormatter TRAN_DTIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OpenBankingClient client;
    private final OpenBankingAuthService authService;
    private final LinkedAccountRepository accountRepository;
    private final BankTranIdGenerator bankTranIdGenerator;

    public BalanceService(
            OpenBankingClient client,
            OpenBankingAuthService authService,
            LinkedAccountRepository accountRepository,
            BankTranIdGenerator bankTranIdGenerator) {
        this.client = client;
        this.authService = authService;
        this.accountRepository = accountRepository;
        this.bankTranIdGenerator = bankTranIdGenerator;
    }

    /**
     * 사용자의 계좌 잔액을 조회한다.
     *
     * @param userSeqNo 사용자 일련번호
     * @param fintechUseNum 조회할 핀테크이용번호. null이면 해당 사용자의 첫 계좌 사용.
     */
    public BalanceResponse getBalance(String userSeqNo, String fintechUseNum) {
        String targetFintechUseNum = resolveFintechUseNum(userSeqNo, fintechUseNum);
        String accessToken = authService.validAccessToken(userSeqNo);

        BalanceApiResponse response = client.getBalance(
                targetFintechUseNum,
                bankTranIdGenerator.generate(),
                LocalDateTime.now(KST).format(TRAN_DTIME),
                accessToken);

        return BalanceResponse.from(response, targetFintechUseNum);
    }

    /** fintechUseNum이 주어지면 사용자 소유인지 확인하고, 없으면 첫 계좌를 사용한다. */
    private String resolveFintechUseNum(String userSeqNo, String fintechUseNum) {
        if (fintechUseNum != null && !fintechUseNum.isBlank()) {
            return accountRepository
                    .findByUserSeqNoAndFintechUseNum(userSeqNo, fintechUseNum)
                    .map(LinkedAccount::getFintechUseNum)
                    .orElseThrow(() -> new OpenBankingNotLinkedException(
                            "해당 사용자의 계좌가 아닙니다: " + fintechUseNum));
        }
        return accountRepository
                .findFirstByUserSeqNoOrderByIdAsc(userSeqNo)
                .map(LinkedAccount::getFintechUseNum)
                .orElseThrow(() -> new OpenBankingNotLinkedException(
                        "연동된 계좌가 없습니다: user_seq_no=" + userSeqNo));
    }
}
