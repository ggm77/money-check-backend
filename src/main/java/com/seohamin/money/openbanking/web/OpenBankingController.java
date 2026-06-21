package com.seohamin.money.openbanking.web;

import com.seohamin.money.openbanking.domain.LinkedAccount;
import com.seohamin.money.openbanking.service.BalanceService;
import com.seohamin.money.openbanking.service.OpenBankingAuthService;
import com.seohamin.money.openbanking.web.dto.AccountResponse;
import com.seohamin.money.openbanking.web.dto.BalanceResponse;
import com.seohamin.money.openbanking.web.dto.LinkResult;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api")
public class OpenBankingController {

    private final OpenBankingAuthService authService;
    private final BalanceService balanceService;

    public OpenBankingController(OpenBankingAuthService authService, BalanceService balanceService) {
        this.authService = authService;
        this.balanceService = balanceService;
    }

    /** KFTC 사용자인증 페이지로 리다이렉트(302). 브라우저에서 동의를 진행한다. */
    @GetMapping("/openbanking/authorize")
    public ResponseEntity<Void> authorize() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authService.buildAuthorizeUrl()))
                .build();
    }

    /** authorize 후 KFTC가 호출하는 콜백. 토큰/계좌를 저장하고 user_seq_no와 계좌목록을 반환한다. */
    @GetMapping("/openbanking/callback")
    public LinkResult callback(@RequestParam String code, @RequestParam String state) {
        String userSeqNo = authService.handleCallback(code, state);
        return new LinkResult(userSeqNo, toAccountResponses(authService.listAccounts(userSeqNo)));
    }

    /** 연동된 계좌 목록 조회. */
    @GetMapping("/accounts")
    public LinkResult accounts(@RequestParam String userSeqNo) {
        return new LinkResult(userSeqNo, toAccountResponses(authService.listAccounts(userSeqNo)));
    }

    /** 잔액조회(핵심). fintechUseNum 생략 시 첫 계좌를 조회한다. */
    @GetMapping("/accounts/balance")
    public BalanceResponse balance(
            @RequestParam String userSeqNo, @RequestParam(required = false) String fintechUseNum) {
        return balanceService.getBalance(userSeqNo, fintechUseNum);
    }

    private static List<AccountResponse> toAccountResponses(List<LinkedAccount> accounts) {
        return accounts.stream().map(AccountResponse::from).toList();
    }
}
