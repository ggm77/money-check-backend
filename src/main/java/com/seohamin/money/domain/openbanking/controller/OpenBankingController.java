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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 오픈뱅킹 잔액조회 서비스 REST 엔드포인트.
 *
 * <p>JWT의 사용자 ID를 기준으로 오픈뱅킹 토큰과 계좌 소유권을 제한한다.
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
    public ResponseEntity<AuthorizeUrlResponseDto> authorize(
            @AuthenticationPrincipal final Long memberId) {
        return ResponseEntity.ok().body(new AuthorizeUrlResponseDto(authService.buildAuthorizeUrl(memberId)));
    }

    /** authorize 후 KFTC가 호출하는 콜백. state에 바인딩된 사용자의 토큰/계좌를 저장한다. */
    @GetMapping("/openbanking/callback")
    public ResponseEntity<LinkResponseDto> callback(
            @RequestParam final String code, @RequestParam final String state) {
        final Long memberId = authService.handleCallback(code, state);
        return ResponseEntity.ok()
                .body(new LinkResponseDto(toAccountResponses(authService.listAccounts(memberId))));
    }

    /** 연동된 계좌 목록 조회. */
    @GetMapping("/accounts")
    public ResponseEntity<LinkResponseDto> accounts(@AuthenticationPrincipal final Long memberId) {
        return ResponseEntity.ok()
                .body(new LinkResponseDto(toAccountResponses(authService.listAccounts(memberId))));
    }

    /** 잔액조회(핵심). fintechUseNum 생략 시 첫 계좌를 조회한다. */
    @GetMapping("/accounts/balance")
    public ResponseEntity<BalanceResponseDto> balance(
            @AuthenticationPrincipal final Long memberId,
            @RequestParam(required = false) final String fintechUseNum) {
        return ResponseEntity.ok().body(balanceService.getBalance(memberId, fintechUseNum));
    }

    private static List<AccountResponseDto> toAccountResponses(final List<LinkedAccount> accounts) {
        return accounts.stream().map(AccountResponseDto::of).toList();
    }
}
