package com.seohamin.money.domain.openbanking.service;

import com.seohamin.money.domain.member.entity.Member;
import com.seohamin.money.domain.member.repository.MemberRepository;
import com.seohamin.money.domain.openbanking.entity.LinkedAccount;
import com.seohamin.money.domain.openbanking.entity.OpenBankingToken;
import com.seohamin.money.domain.openbanking.repository.LinkedAccountRepository;
import com.seohamin.money.domain.openbanking.repository.OpenBankingTokenRepository;
import com.seohamin.money.global.config.OpenBankingProperties;
import com.seohamin.money.global.exception.CustomException;
import com.seohamin.money.global.exception.constants.ExceptionCode;
import com.seohamin.money.global.client.OpenBankingClient;
import com.seohamin.money.global.client.dto.TokenResponse;
import com.seohamin.money.global.client.dto.UserInfoResponse;
import com.seohamin.money.global.support.OAuthStateStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
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
    private final MemberRepository memberRepository;
    private final OpenBankingTokenRepository tokenRepository;
    private final LinkedAccountRepository accountRepository;

    /** KFTC 사용자인증(authorize) URL을 생성한다. state는 발급 후 저장되어 콜백에서 검증된다. */
    public String buildAuthorizeUrl(final Long memberId) {
        final String state = stateStore.issue(memberId);
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
     * @return 연동을 시작한 서비스 사용자 ID
     */
    @Transactional
    public Long handleCallback(final String code, final String state) {
        final Long memberId = stateStore
                .consume(state)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_OAUTH_STATE));
        final Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_OAUTH_STATE));

        final TokenResponse token = client.issueToken(code);
        saveOrUpdateToken(member, token);

        final UserInfoResponse userInfo = client.getUserInfo(token.userSeqNo(), token.accessToken());
        saveNewAccounts(member, token.userSeqNo(), userInfo);

        return memberId;
    }

    /** 유효한 access token을 반환한다. 만료 임박 시 refresh_token으로 갱신 후 저장한다. */
    @Transactional
    public String validAccessToken(final Long memberId) {
        final OpenBankingToken token = tokenRepository
                .findByMemberId(memberId)
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
    public List<LinkedAccount> listAccounts(final Long memberId) {
        final List<LinkedAccount> accounts = accountRepository.findByMemberId(memberId);
        if (accounts.isEmpty()) {
            throw new CustomException(ExceptionCode.OPENBANKING_NOT_LINKED);
        }
        return accounts;
    }

    private void saveOrUpdateToken(final Member member, final TokenResponse token) {
        tokenRepository.findByUserSeqNo(token.userSeqNo()).ifPresent(existing -> {
            if (!Objects.equals(existing.getMember().getId(), member.getId())) {
                throw new CustomException(ExceptionCode.OPENBANKING_ALREADY_LINKED);
            }
        });

        tokenRepository
                .findByMemberId(member.getId())
                .ifPresentOrElse(
                        existing -> {
                            if (!Objects.equals(existing.getUserSeqNo(), token.userSeqNo())) {
                                throw new CustomException(ExceptionCode.OPENBANKING_ALREADY_LINKED);
                            }
                            existing.renew(
                                    token.accessToken(),
                                    token.refreshToken(),
                                    expiresAt(token),
                                    token.scope());
                        },
                        () -> tokenRepository.save(OpenBankingToken.builder()
                                .member(member)
                                .userSeqNo(token.userSeqNo())
                                .accessToken(token.accessToken())
                                .refreshToken(token.refreshToken())
                                .tokenType(token.tokenType())
                                .scope(token.scope())
                                .expiresAt(expiresAt(token))
                                .build()));
    }

    private void saveNewAccounts(
            final Member member,
            final String userSeqNo,
            final UserInfoResponse userInfo
    ) {
        if (userInfo.resList() == null) {
            return;
        }
        for (final UserInfoResponse.Account account : userInfo.resList()) {
            if (account.fintechUseNum() == null) {
                continue;
            }

            final LinkedAccount existing = accountRepository
                    .findByFintechUseNum(account.fintechUseNum())
                    .orElse(null);
            if (existing != null) {
                if (!Objects.equals(existing.getMember().getId(), member.getId())) {
                    throw new CustomException(ExceptionCode.OPENBANKING_ALREADY_LINKED);
                }
                continue;
            }

            accountRepository.save(LinkedAccount.builder()
                    .member(member)
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
