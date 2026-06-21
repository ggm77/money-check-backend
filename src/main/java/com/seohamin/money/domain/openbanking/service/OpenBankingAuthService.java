package com.seohamin.money.domain.openbanking.service;

import com.seohamin.money.domain.openbanking.client.OpenBankingClient;
import com.seohamin.money.domain.openbanking.client.dto.TokenResponse;
import com.seohamin.money.domain.openbanking.client.dto.UserInfoResponse;
import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.entity.OpenBankingToken;
import com.seohamin.money.domain.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.domain.openbanking.repository.OpenBankingTokenRepository;
import com.seohamin.money.domain.openbanking.support.OAuthStateStore;
import com.seohamin.money.global.config.OpenBankingProperties;
import com.seohamin.money.global.exception.CustomException;
import com.seohamin.money.global.exception.constants.ExceptionCode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OpenBankingAuthService {

    /** 만료 이 시간 이내면 미리 갱신한다. */
    private static final Duration REFRESH_BUFFER = Duration.ofSeconds(60);

    private final OpenBankingClient client;
    private final OpenBankingProperties properties;
    private final OAuthStateStore stateStore;
    private final OpenBankingTokenRepository tokenRepository;
    private final LinkedAccountRepository accountRepository;

    /** KFTC 사용자인증(authorize) URL을 생성한다. state는 발급 후 저장되어 콜백에서 검증된다. */
    public String buildAuthorizeUrl() {
        final String state = stateStore.issue();
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
    public String handleCallback(final String code, final String state) {
        if (!stateStore.consume(state)) {
            throw new CustomException(ExceptionCode.INVALID_OAUTH_STATE);
        }

        final TokenResponse token = client.issueToken(code);
        saveOrUpdateToken(token);

        final UserInfoResponse userInfo = client.getUserInfo(token.userSeqNo(), token.accessToken());
        saveNewAccounts(token.userSeqNo(), userInfo);

        return token.userSeqNo();
    }

    /** 유효한 access token을 반환한다. 만료 임박 시 refresh_token으로 갱신 후 저장한다. */
    @Transactional
    public String validAccessToken(final String userSeqNo) {
        final OpenBankingToken token = tokenRepository
                .findByUserSeqNo(userSeqNo)
                .orElseThrow(() -> new CustomException(ExceptionCode.OPENBANKING_NOT_LINKED));

        if (token.isExpiringWithin(REFRESH_BUFFER)) {
            final TokenResponse refreshed = client.refreshToken(token.getRefreshToken());
            token.renew(
                    refreshed.accessToken(),
                    refreshed.refreshToken(),
                    expiresAt(refreshed),
                    refreshed.scope());
        }
        return token.getAccessToken();
    }

    @Transactional(readOnly = true)
    public List<LinkedAccount> listAccounts(final String userSeqNo) {
        final List<LinkedAccount> accounts = accountRepository.findByUserSeqNo(userSeqNo);
        if (accounts.isEmpty()) {
            throw new CustomException(ExceptionCode.OPENBANKING_NOT_LINKED);
        }
        return accounts;
    }

    private void saveOrUpdateToken(final TokenResponse token) {
        tokenRepository
                .findByUserSeqNo(token.userSeqNo())
                .ifPresentOrElse(
                        existing -> existing.renew(
                                token.accessToken(), token.refreshToken(), expiresAt(token), token.scope()),
                        () -> tokenRepository.save(OpenBankingToken.builder()
                                .userSeqNo(token.userSeqNo())
                                .accessToken(token.accessToken())
                                .refreshToken(token.refreshToken())
                                .tokenType(token.tokenType())
                                .scope(token.scope())
                                .expiresAt(expiresAt(token))
                                .build()));
    }

    private void saveNewAccounts(final String userSeqNo, final UserInfoResponse userInfo) {
        if (userInfo.resList() == null) {
            return;
        }
        for (final UserInfoResponse.Account account : userInfo.resList()) {
            if (account.fintechUseNum() == null
                    || accountRepository.existsByFintechUseNum(account.fintechUseNum())) {
                continue;
            }
            accountRepository.save(LinkedAccount.builder()
                    .userSeqNo(userSeqNo)
                    .fintechUseNum(account.fintechUseNum())
                    .bankCodeStd(account.bankCodeStd())
                    .bankName(account.bankName())
                    .accountAlias(account.accountAlias())
                    .accountNumMasked(account.accountNumMasked())
                    .accountHolderName(account.accountHolderName())
                    .build());
        }
    }

    private static Instant expiresAt(final TokenResponse token) {
        final long seconds = token.expiresIn() != null ? token.expiresIn() : 0L;
        return Instant.now().plusSeconds(seconds);
    }
}
