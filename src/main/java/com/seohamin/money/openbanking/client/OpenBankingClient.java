package com.seohamin.money.openbanking.client;

import com.seohamin.money.config.OpenBankingProperties;
import com.seohamin.money.openbanking.client.dto.BalanceApiResponse;
import com.seohamin.money.openbanking.client.dto.TokenResponse;
import com.seohamin.money.openbanking.client.dto.UserInfoResponse;
import java.nio.charset.StandardCharsets;
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
public class OpenBankingClient {

    private static final String SUCCESS_CODE = "A0000";

    private final RestClient restClient;
    private final OpenBankingProperties properties;

    public OpenBankingClient(RestClient openBankingRestClient, OpenBankingProperties properties) {
        this.restClient = openBankingRestClient;
        this.properties = properties;
    }

    /** 인증코드(code)로 access/refresh 토큰을 발급한다. (grant_type=authorization_code) */
    public TokenResponse issueToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("grant_type", "authorization_code");

        TokenResponse response = postToken(form, "토큰 발급");
        return requireToken(response, "토큰 발급");
    }

    /** refresh_token으로 access 토큰을 재발급한다. (grant_type=refresh_token) */
    public TokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("refresh_token", refreshToken);
        form.add("scope", properties.scope());
        form.add("grant_type", "refresh_token");

        TokenResponse response = postToken(form, "토큰 갱신");
        return requireToken(response, "토큰 갱신");
    }

    /** 사용자정보조회(user/me): 등록 계좌 목록(fintech_use_num 포함)을 가져온다. */
    public UserInfoResponse getUserInfo(String userSeqNo, String accessToken) {
        UserInfoResponse response = restClient
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
            String fintechUseNum, String bankTranId, String tranDtime, String accessToken) {
        BalanceApiResponse response = restClient
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

    private TokenResponse postToken(MultiValueMap<String, String> form, String operation) {
        return restClient
                .post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, httpErrorHandler(operation))
                .body(TokenResponse.class);
    }

    private TokenResponse requireToken(TokenResponse response, String operation) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            String code = response == null ? "null" : response.rspCode();
            String message = response == null ? "empty response" : response.rspMessage();
            throw new OpenBankingApiException(operation + " 실패", code, message);
        }
        return response;
    }

    private void requireSuccessCode(String rspCode, String rspMessage, String operation) {
        if (!SUCCESS_CODE.equals(rspCode)) {
            throw new OpenBankingApiException(operation + " 실패", rspCode, rspMessage);
        }
    }

    private RestClient.ResponseSpec.ErrorHandler httpErrorHandler(String operation) {
        return (request, response) -> {
            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            throw new OpenBankingApiException(
                    operation + " HTTP 오류", String.valueOf(response.getStatusCode().value()), body);
        };
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
