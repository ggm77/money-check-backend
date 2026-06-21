package com.seohamin.money.domain.openbanking.client;

import com.seohamin.money.domain.openbanking.client.dto.BalanceApiResponse;
import com.seohamin.money.domain.openbanking.client.dto.TokenResponse;
import com.seohamin.money.domain.openbanking.client.dto.UserInfoResponse;
import com.seohamin.money.global.config.OpenBankingProperties;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * KFTC 오픈뱅킹 아웃바운드 호출 래퍼. 토큰 발급/갱신, 사용자정보조회, 잔액조회 4종을 제공하며
 * HTTP 오류와 비정상 rsp_code를 {@link OpenBankingApiException}으로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class OpenBankingClient {

    private static final String SUCCESS_CODE = "A0000";

    private final RestClient openBankingRestClient;
    private final OpenBankingProperties properties;

    /** 인증코드(code)로 access/refresh 토큰을 발급한다. (grant_type=authorization_code) */
    public TokenResponse issueToken(final String code) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("grant_type", "authorization_code");

        return requireToken(postToken(form, "토큰 발급"), "토큰 발급");
    }

    /** refresh_token으로 access 토큰을 재발급한다. (grant_type=refresh_token) */
    public TokenResponse refreshToken(final String refreshToken) {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("refresh_token", refreshToken);
        form.add("scope", properties.scope());
        form.add("grant_type", "refresh_token");

        return requireToken(postToken(form, "토큰 갱신"), "토큰 갱신");
    }

    /** 사용자정보조회(user/me): 등록 계좌 목록(fintech_use_num 포함)을 가져온다. */
    public UserInfoResponse getUserInfo(final String userSeqNo, final String accessToken) {
        final UserInfoResponse response = openBankingRestClient
                .get()
                .uri(uri -> uri.path("/v2.0/user/me").queryParam("user_seq_no", userSeqNo).build())
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, httpErrorHandler("사용자정보조회"))
                .body(UserInfoResponse.class);

        requireSuccessCode(response == null ? null : response.rspCode(),
                response == null ? null : response.rspMessage(), "사용자정보조회");
        return response;
    }

    /** 잔액조회: fintech_use_num에 대한 계좌 잔액을 가져온다. */
    public BalanceApiResponse getBalance(
            final String fintechUseNum, final String bankTranId, final String tranDtime, final String accessToken) {
        final BalanceApiResponse response = openBankingRestClient
                .get()
                .uri(uri -> uri.path("/v2.0/account/balance/fin_num")
                        .queryParam("bank_tran_id", bankTranId)
                        .queryParam("fintech_use_num", fintechUseNum)
                        .queryParam("tran_dtime", tranDtime)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .retrieve()
                .onStatus(HttpStatusCode::isError, httpErrorHandler("잔액조회"))
                .body(BalanceApiResponse.class);

        requireSuccessCode(response == null ? null : response.rspCode(),
                response == null ? null : response.rspMessage(), "잔액조회");
        return response;
    }

    private TokenResponse postToken(final MultiValueMap<String, String> form, final String operation) {
        return openBankingRestClient
                .post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, httpErrorHandler(operation))
                .body(TokenResponse.class);
    }

    private TokenResponse requireToken(final TokenResponse response, final String operation) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            final String code = response == null ? "null" : response.rspCode();
            final String message = response == null ? "empty response" : response.rspMessage();
            throw new OpenBankingApiException(operation + " 실패", code, message);
        }
        return response;
    }

    private void requireSuccessCode(final String rspCode, final String rspMessage, final String operation) {
        if (!SUCCESS_CODE.equals(rspCode)) {
            throw new OpenBankingApiException(operation + " 실패", rspCode, rspMessage);
        }
    }

    private RestClient.ResponseSpec.ErrorHandler httpErrorHandler(final String operation) {
        return (request, response) -> {
            final String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            throw new OpenBankingApiException(
                    operation + " HTTP 오류", String.valueOf(response.getStatusCode().value()), body);
        };
    }

    private static String bearer(final String accessToken) {
        return "Bearer " + accessToken;
    }
}
