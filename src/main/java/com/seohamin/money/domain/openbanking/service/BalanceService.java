package com.seohamin.money.domain.openbanking.service;

import com.seohamin.money.domain.openbanking.client.OpenBankingClient;
import com.seohamin.money.domain.openbanking.client.dto.BalanceApiResponse;
import com.seohamin.money.domain.openbanking.dto.BalanceResponseDto;
import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.domain.openbanking.support.BankTranIdGenerator;
import com.seohamin.money.global.exception.CustomException;
import com.seohamin.money.global.exception.constants.ExceptionCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 잔액조회 오케스트레이션: 유효 토큰 확보 → 요청 식별자 생성 → 잔액 API 호출 → 응답 매핑. */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private static final DateTimeFormatter TRAN_DTIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final OpenBankingClient client;
    private final OpenBankingAuthService authService;
    private final LinkedAccountRepository accountRepository;
    private final BankTranIdGenerator bankTranIdGenerator;

    /**
     * 사용자의 계좌 잔액을 조회한다.
     *
     * @param memberId 로그인 사용자 ID
     * @param fintechUseNum 조회할 핀테크이용번호
     */
    public BalanceResponseDto getBalance(final Long memberId, final String fintechUseNum) {
        final String targetFintechUseNum = resolveFintechUseNum(memberId, fintechUseNum);
        final String accessToken = authService.validAccessToken(memberId);

        final BalanceApiResponse response = client.getBalance(
                targetFintechUseNum,
                bankTranIdGenerator.generate(),
                LocalDateTime.now(KST).format(TRAN_DTIME),
                accessToken);

        return BalanceResponseDto.of(response, targetFintechUseNum);
    }

    /** fintechUseNum 필수값과 사용자 소유 여부를 확인한다. */
    private String resolveFintechUseNum(final Long memberId, final String fintechUseNum) {
        if (fintechUseNum == null || fintechUseNum.isBlank()) {
            throw new CustomException(ExceptionCode.INVALID_REQUEST);
        }
        return accountRepository
                .findByMemberIdAndFintechUseNum(memberId, fintechUseNum)
                .map(LinkedAccount::getFintechUseNum)
                .orElseThrow(() -> new CustomException(ExceptionCode.OPENBANKING_NOT_LINKED));
    }
}
