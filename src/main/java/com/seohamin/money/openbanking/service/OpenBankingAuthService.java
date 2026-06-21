package com.seohamin.money.openbanking.service;

import com.seohamin.money.config.OpenBankingProperties;
import com.seohamin.money.openbanking.client.OpenBankingApiException;
import com.seohamin.money.openbanking.client.OpenBankingClient;
import com.seohamin.money.openbanking.client.dto.TokenResponse;
import com.seohamin.money.openbanking.client.dto.UserInfoResponse;
import com.seohamin.money.openbanking.domain.LinkedAccount;
import com.seohamin.money.openbanking.domain.OpenBankingToken;
import com.seohamin.money.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.openbanking.repository.OpenBankingTokenRepository;
import com.seohamin.money.openbanking.support.OAuthStateStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 오픈뱅킹 사용자 인증/연동 흐름을 담당한다.
 * <ul>
 *   <li>authorize URL 생성(+state 발급)</li>
 *   <li>콜백 처리: 인증코드→토큰 교환→사용자정보조회→토큰/계좌 영속화</li>
 *   <li>유효한 access token 확보(만료 임박 시 자동 갱신)</li>
 * </ul>
 */
@Service
public class OpenBankingAuthService {

    /** 만료 이 시간 이내면 미리 갱신한다. */
    private static final Duration REFRESH_BUFFER = Duration.ofSeconds(60);

    private final OpenBankingClient client;
    private final OpenBankingProperties properties;
    private final OAuthStateStore stateStore;
    private final OpenBankingTokenRepository tokenRepository;
    private final LinkedAccountRepository accountRepository;

    public OpenBankingAuthService(
            OpenBankingClient client,
            OpenBankingProperties properties,
            OAuthStateStore stateStore,
            OpenBankingTokenRepository tokenRepository,
            LinkedAccountRepository accountRepository) {
        this.client = client;
        this.properties = properties;
        this.stateStore = stateStore;
        this.tokenRepository = tokenRepository;
        this.accountRepository = accountRepository;
    }

    /** KFTC 사용자인증(authorize) URL을 생성한다. state는 발급 후 저장되어 콜백에서 검증된다. */
    public String buildAuthorizeUrl() {
        String state = stateStore.issue();
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path("/oauth/2.0/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", properties.scope())
                .queryParam("state", state)
                .queryParam("auth_type", "0")
                .encode()
                .toUriString();
    }

    /**
     * 콜백 처리: state 검증 → 토큰 발급 → 사용자정보조회 → 토큰/계좌 저장.
     *
     * @return 연동된 user_seq_no
     */
    @Transactional
    public String handleCallback(String code, String state) {
        if (!stateStore.consume(state)) {
            throw new OpenBankingApiException("state 검증 실패(위조/만료)", "invalid_state", state);
        }

        TokenResponse token = client.issueToken(code);
        saveOrUpdateToken(token);

        UserInfoResponse userInfo = client.getUserInfo(token.userSeqNo(), token.accessToken());
        saveNewAccounts(token.userSeqNo(), userInfo);

        return token.userSeqNo();
    }

    /** 유효한 access token을 반환한다. 만료 임박 시 refresh_token으로 갱신 후 저장한다. */
    @Transactional
    public String validAccessToken(String userSeqNo) {
        OpenBankingToken token = tokenRepository
                .findByUserSeqNo(userSeqNo)
                .orElseThrow(() -> new OpenBankingNotLinkedException(
                        "연동된 토큰이 없습니다: user_seq_no=" + userSeqNo));

        if (token.isExpiringWithin(REFRESH_BUFFER)) {
            TokenResponse refreshed = client.refreshToken(token.getRefreshToken());
            token.renew(
                    refreshed.accessToken(),
                    refreshed.refreshToken(),
                    expiresAt(refreshed),
                    refreshed.scope());
        }
        return token.getAccessToken();
    }

    @Transactional(readOnly = true)
    public List<LinkedAccount> listAccounts(String userSeqNo) {
        List<LinkedAccount> accounts = accountRepository.findByUserSeqNo(userSeqNo);
        if (accounts.isEmpty()) {
            throw new OpenBankingNotLinkedException("연동된 계좌가 없습니다: user_seq_no=" + userSeqNo);
        }
        return accounts;
    }

    private void saveOrUpdateToken(TokenResponse token) {
        tokenRepository
                .findByUserSeqNo(token.userSeqNo())
                .ifPresentOrElse(
                        existing -> existing.renew(
                                token.accessToken(), token.refreshToken(), expiresAt(token), token.scope()),
                        () -> tokenRepository.save(new OpenBankingToken(
                                token.userSeqNo(),
                                token.accessToken(),
                                token.refreshToken(),
                                token.tokenType(),
                                token.scope(),
                                expiresAt(token))));
    }

    private void saveNewAccounts(String userSeqNo, UserInfoResponse userInfo) {
        if (userInfo.resList() == null) {
            return;
        }
        for (UserInfoResponse.Account account : userInfo.resList()) {
            if (account.fintechUseNum() == null
                    || accountRepository.existsByFintechUseNum(account.fintechUseNum())) {
                continue;
            }
            accountRepository.save(new LinkedAccount(
                    userSeqNo,
                    account.fintechUseNum(),
                    account.bankCodeStd(),
                    account.bankName(),
                    account.accountAlias(),
                    account.accountNumMasked(),
                    account.accountHolderName()));
        }
    }

    private static Instant expiresAt(TokenResponse token) {
        long seconds = token.expiresIn() != null ? token.expiresIn() : 0L;
        return Instant.now().plusSeconds(seconds);
    }
}
