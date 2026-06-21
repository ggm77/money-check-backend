package com.seohamin.money.domain.openbanking.controller;

import com.seohamin.money.domain.openbanking.dto.AccountResponseDto;
import com.seohamin.money.domain.openbanking.dto.AuthorizeUrlResponseDto;
import com.seohamin.money.domain.openbanking.dto.BalanceResponseDto;
import com.seohamin.money.domain.openbanking.dto.LinkResponseDto;
import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.service.BalanceService;
import com.seohamin.money.domain.openbanking.service.OpenBankingAuthService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오픈뱅킹 잔액조회 서비스 REST 엔드포인트.
 *
 * <p>유저 식별은 KFTC가 발급한 user_seq_no를 클라이언트가 전달하는 방식(데모). 실제 서비스에서는
 * 자체 로그인 세션/유저 PK에 바인딩해야 한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class OpenBankingController {

    private final OpenBankingAuthService authService;
    private final BalanceService balanceService;

    /**
     * KFTC 사용자인증 URL을 JSON으로 반환한다. 반환된 authorizeUrl을 브라우저 주소창에서 열어 동의를 진행한다.
     * (302 리다이렉트가 아니라 JSON이므로 Swagger의 fetch가 KFTC로 따라가며 나던 CORS 오류가 없다.)
     */
    @GetMapping("/openbanking/authorize")
    public ResponseEntity<AuthorizeUrlResponseDto> authorize() {
        return ResponseEntity.ok().body(new AuthorizeUrlResponseDto(authService.buildAuthorizeUrl()));
    }

    /** authorize 후 KFTC가 호출하는 콜백. 토큰/계좌를 저장하고 user_seq_no와 계좌목록을 반환한다. */
    @GetMapping("/openbanking/callback")
    public ResponseEntity<LinkResponseDto> callback(
            @RequestParam final String code, @RequestParam final String state) {
        final String userSeqNo = authService.handleCallback(code, state);
        return ResponseEntity.ok()
                .body(new LinkResponseDto(userSeqNo, toAccountResponses(authService.listAccounts(userSeqNo))));
    }

    /** 연동된 계좌 목록 조회. */
    @GetMapping("/accounts")
    public ResponseEntity<LinkResponseDto> accounts(@RequestParam final String userSeqNo) {
        return ResponseEntity.ok()
                .body(new LinkResponseDto(userSeqNo, toAccountResponses(authService.listAccounts(userSeqNo))));
    }

    /** 잔액조회(핵심). fintechUseNum 생략 시 첫 계좌를 조회한다. */
    @GetMapping("/accounts/balance")
    public ResponseEntity<BalanceResponseDto> balance(
            @RequestParam final String userSeqNo, @RequestParam(required = false) final String fintechUseNum) {
        return ResponseEntity.ok().body(balanceService.getBalance(userSeqNo, fintechUseNum));
    }

    private static List<AccountResponseDto> toAccountResponses(final List<LinkedAccount> accounts) {
        return accounts.stream().map(AccountResponseDto::of).toList();
    }
}
