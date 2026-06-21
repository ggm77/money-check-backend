package com.seohamin.money.domain.openbanking.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@code POST /oauth/2.0/token} 응답 (토큰 발급/갱신 공통).
 * 성공 시 access_token 등이, 실패 시 rsp_code/rsp_message가 채워진다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") String scope,
        @JsonProperty("user_seq_no") String userSeqNo,
        @JsonProperty("rsp_code") String rspCode,
        @JsonProperty("rsp_message") String rspMessage) {}
